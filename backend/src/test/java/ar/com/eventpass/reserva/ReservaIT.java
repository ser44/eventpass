package ar.com.eventpass.reserva;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.JsonNode;

import ar.com.eventpass.IntegracionBaseIT;
import ar.com.eventpass.evento.Evento;
import ar.com.eventpass.evento.EventoRepository;
import ar.com.eventpass.usuario.dto.LoginRequest;

class ReservaIT extends IntegracionBaseIT {

    @Autowired
    private EventoRepository eventoRepo;

    private String token(String email, String password) throws Exception {
        var login = new LoginRequest(email, password);
        var resp = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(login)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("token").asText();
    }

    private UUID crearYPublicar(int cupo) throws Exception {
        String tOrga = token("orga@eventpass.dev", "Orga123!");
        Instant futuro = Instant.now().plus(20, ChronoUnit.DAYS);
        String body = """
            {"titulo":"Concurrencia %s","descripcion":"d","lugar":"Estadio","fechaInicio":"%s","cupoTotal":%d}
            """.formatted(UUID.randomUUID(), futuro, cupo);
        var resp = mvc.perform(post("/events").header("Authorization", "Bearer " + tOrga)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(json.readTree(resp).get("id").asText());
        mvc.perform(post("/events/" + id + "/publish").header("Authorization", "Bearer " + tOrga))
            .andExpect(status().isOk());
        return id;
    }

    @Test
    @DisplayName("CP03: 200 reservas concurrentes de 1 sobre cupo 50 -> 50 ok, 150 sin cupo, cupo final 0")
    void cp03NoSobreventa() throws Exception {
        UUID eventoId = crearYPublicar(50);
        String tAsis = token("asistente1@eventpass.dev", "Asis123!");

        int n = 200;
        ExecutorService pool = Executors.newFixedThreadPool(32);
        List<Callable<Integer>> tareas = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            tareas.add(() -> mvc.perform(post("/events/" + eventoId + "/reservations")
                    .header("Authorization", "Bearer " + tAsis)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":1}"))
                .andReturn().getResponse().getStatus());
        }
        List<Future<Integer>> resultados = pool.invokeAll(tareas);
        pool.shutdown();

        long ok = contar(resultados, 201);
        long sinCupo = contar(resultados, 409);
        org.assertj.core.api.Assertions.assertThat(ok).isEqualTo(50);
        org.assertj.core.api.Assertions.assertThat(sinCupo).isEqualTo(150);

        Evento e = eventoRepo.findById(eventoId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(e.getCupoDisponible()).isZero();
    }

    @Test
    @DisplayName("Idempotencia: misma Idempotency-Key produce una sola reserva")
    void idempotencia() throws Exception {
        UUID eventoId = crearYPublicar(10);
        String tAsis = token("asistente2@eventpass.dev", "Asis123!");
        String clave = UUID.randomUUID().toString();

        String r1 = mvc.perform(post("/events/" + eventoId + "/reservations")
                .header("Authorization", "Bearer " + tAsis).header("Idempotency-Key", clave)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":2}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String r2 = mvc.perform(post("/events/" + eventoId + "/reservations")
                .header("Authorization", "Bearer " + tAsis).header("Idempotency-Key", clave)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":2}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String id1 = json.readTree(r1).get("id").asText();
        String id2 = json.readTree(r2).get("id").asText();
        org.assertj.core.api.Assertions.assertThat(id1).isEqualTo(id2);

        Evento e = eventoRepo.findById(eventoId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(e.getCupoDisponible()).isEqualTo(8); // descuenta 2, no 4
    }

    @Test
    @DisplayName("Flujo: reservar -> confirmar emite entradas -> aparecen en /me/tickets")
    void flujoReservaConfirmacion() throws Exception {
        UUID eventoId = crearYPublicar(10);
        String tAsis = token("asistente3@eventpass.dev", "Asis123!");

        var reserva = mvc.perform(post("/events/" + eventoId + "/reservations")
                .header("Authorization", "Bearer " + tAsis)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":2}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String reservaId = json.readTree(reserva).get("id").asText();

        mvc.perform(post("/reservations/" + reservaId + "/confirm").header("Authorization", "Bearer " + tAsis))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
            .andExpect(jsonPath("$.entradas.length()").value(2));

        var tickets = mvc.perform(get("/me/tickets").header("Authorization", "Bearer " + tAsis))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode arr = json.readTree(tickets);
        org.assertj.core.api.Assertions.assertThat(arr.size()).isGreaterThanOrEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(arr.get(0).get("codigo").asText()).startsWith("EVP-");
    }

    @Test
    @DisplayName("Reservar sobre un evento en BORRADOR devuelve 422 EVENTO_NO_RESERVABLE")
    void reservarBorrador() throws Exception {
        String tOrga = token("orga@eventpass.dev", "Orga123!");
        Instant futuro = Instant.now().plus(20, ChronoUnit.DAYS);
        String body = """
            {"titulo":"Borrador %s","descripcion":"d","lugar":"X","fechaInicio":"%s","cupoTotal":10}
            """.formatted(UUID.randomUUID(), futuro);
        var resp = mvc.perform(post("/events").header("Authorization", "Bearer " + tOrga)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(json.readTree(resp).get("id").asText());

        String tAsis = token("asistente4@eventpass.dev", "Asis123!");
        mvc.perform(post("/events/" + id + "/reservations").header("Authorization", "Bearer " + tAsis)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":1}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("EVENTO_NO_RESERVABLE"));
    }

    private long contar(List<Future<Integer>> resultados, int status) throws Exception {
        long c = 0;
        for (Future<Integer> f : resultados) {
            if (f.get() == status) {
                c++;
            }
        }
        return c;
    }
}
