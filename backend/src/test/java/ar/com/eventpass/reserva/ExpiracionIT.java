package ar.com.eventpass.reserva;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import ar.com.eventpass.IntegracionBaseIT;
import ar.com.eventpass.evento.Evento;
import ar.com.eventpass.evento.EventoRepository;
import ar.com.eventpass.usuario.dto.LoginRequest;

class ExpiracionIT extends IntegracionBaseIT {

    @Autowired private ExpiracionReservasJob job;
    @Autowired private ReservaRepository reservaRepo;
    @Autowired private EventoRepository eventoRepo;

    private String token(String email, String password) throws Exception {
        var login = new LoginRequest(email, password);
        var resp = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(login)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("token").asText();
    }

    @Test
    @DisplayName("CP04: una reserva vencida hace 1 min se expira y restituye el cupo (+2)")
    void cp04() throws Exception {
        String tOrga = token("orga@eventpass.dev", "Orga123!");
        Instant futuro = Instant.now().plus(20, ChronoUnit.DAYS);
        String body = """
            {"titulo":"Expira %s","descripcion":"d","lugar":"Aula","fechaInicio":"%s","cupoTotal":10}
            """.formatted(UUID.randomUUID(), futuro);
        var evResp = mvc.perform(post("/events").header("Authorization", "Bearer " + tOrga)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID eventoId = UUID.fromString(json.readTree(evResp).get("id").asText());
        mvc.perform(post("/events/" + eventoId + "/publish").header("Authorization", "Bearer " + tOrga))
            .andExpect(status().isOk());

        String tAsis = token("asistente5@eventpass.dev", "Asis123!");
        var resResp = mvc.perform(post("/events/" + eventoId + "/reservations")
                .header("Authorization", "Bearer " + tAsis)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":2}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID reservaId = UUID.fromString(json.readTree(resResp).get("id").asText());

        // cupo quedó en 8
        org.assertj.core.api.Assertions.assertThat(eventoRepo.findById(eventoId).orElseThrow().getCupoDisponible())
            .isEqualTo(8);

        // Forzamos el vencimiento (expira_en hace 1 min) y corremos el job.
        Reserva r = reservaRepo.findById(reservaId).orElseThrow();
        ReflectionTestUtils.setField(r, "expiraEn", Instant.now().minus(1, ChronoUnit.MINUTES));
        reservaRepo.saveAndFlush(r);

        int expiradas = job.expirarVencidas();

        org.assertj.core.api.Assertions.assertThat(expiradas).isGreaterThanOrEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(reservaRepo.findById(reservaId).orElseThrow().getEstado())
            .isEqualTo(EstadoReserva.EXPIRADA);
        org.assertj.core.api.Assertions.assertThat(eventoRepo.findById(eventoId).orElseThrow().getCupoDisponible())
            .isEqualTo(10);
    }
}
