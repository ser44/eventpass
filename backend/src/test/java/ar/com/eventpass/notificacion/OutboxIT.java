package ar.com.eventpass.notificacion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ar.com.eventpass.IntegracionBaseIT;
import ar.com.eventpass.usuario.dto.LoginRequest;

/** Outbox con SMTP caído: la reserva queda CONFIRMADA y el mensaje sigue PENDIENTE con reintentos. */
class OutboxIT extends IntegracionBaseIT {

    @MockBean private CanalNotificacion canal;
    @Autowired private DespachadorOutbox despachador;
    @Autowired private MensajeOutboxRepository outboxRepo;

    private String token(String email, String password) throws Exception {
        var login = new LoginRequest(email, password);
        var resp = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(login)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("token").asText();
    }

    @Test
    @DisplayName("Confirmar con SMTP caído: reserva CONFIRMADA y outbox sigue PENDIENTE con intentos+1")
    void outboxSmtpCaido() throws Exception {
        doThrow(new RuntimeException("SMTP caido")).when(canal).enviar(any());

        String tOrga = token("orga@eventpass.dev", "Orga123!");
        Instant futuro = Instant.now().plus(20, ChronoUnit.DAYS);
        String body = """
            {"titulo":"Outbox %s","descripcion":"d","lugar":"Aula","fechaInicio":"%s","cupoTotal":10}
            """.formatted(UUID.randomUUID(), futuro);
        var evResp = mvc.perform(post("/events").header("Authorization", "Bearer " + tOrga)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID eventoId = UUID.fromString(json.readTree(evResp).get("id").asText());
        mvc.perform(post("/events/" + eventoId + "/publish").header("Authorization", "Bearer " + tOrga))
            .andExpect(status().isOk());

        String tAsis = token("asistente1@eventpass.dev", "Asis123!");
        var resResp = mvc.perform(post("/events/" + eventoId + "/reservations")
                .header("Authorization", "Bearer " + tAsis)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":1}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID reservaId = UUID.fromString(json.readTree(resResp).get("id").asText());

        long pendientesAntes = outboxRepo.countByEstado(EstadoOutbox.PENDIENTE);

        // Confirmar -> OutboxListener inserta el mensaje en la misma transacción.
        mvc.perform(post("/reservations/" + reservaId + "/confirm").header("Authorization", "Bearer " + tAsis))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        org.assertj.core.api.Assertions.assertThat(outboxRepo.countByEstado(EstadoOutbox.PENDIENTE))
            .isEqualTo(pendientesAntes + 1);

        // El despachador intenta enviar y falla: el mensaje sigue PENDIENTE con intentos incrementado.
        despachador.despachar();

        List<MensajeOutbox> todos = outboxRepo.findAll();
        MensajeOutbox msg = todos.stream()
            .filter(m -> m.getIntentos() >= 1 && m.getEstado() == EstadoOutbox.PENDIENTE)
            .findFirst().orElseThrow();
        org.assertj.core.api.Assertions.assertThat(msg.getIntentos()).isGreaterThanOrEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(msg.getEstado()).isEqualTo(EstadoOutbox.PENDIENTE);
    }
}
