package ar.com.eventpass.entrada;

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
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.JsonNode;

import ar.com.eventpass.IntegracionBaseIT;
import ar.com.eventpass.usuario.dto.LoginRequest;

class CheckInIT extends IntegracionBaseIT {

    private String token(String email, String password) throws Exception {
        var login = new LoginRequest(email, password);
        var resp = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(login)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("token").asText();
    }

    private UUID crearPublicar(String tOrga, int cupo) throws Exception {
        Instant futuro = Instant.now().plus(20, ChronoUnit.DAYS);
        String body = """
            {"titulo":"CheckIn %s","descripcion":"d","lugar":"Puerta","fechaInicio":"%s","cupoTotal":%d}
            """.formatted(UUID.randomUUID(), futuro, cupo);
        var resp = mvc.perform(post("/events").header("Authorization", "Bearer " + tOrga)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(json.readTree(resp).get("id").asText());
        mvc.perform(post("/events/" + id + "/publish").header("Authorization", "Bearer " + tOrga))
            .andExpect(status().isOk());
        return id;
    }

    /** Reserva `cantidad` y confirma; devuelve el primer código de entrada. */
    private String reservarConfirmar(String tAsis, UUID eventoId, int cantidad) throws Exception {
        var res = mvc.perform(post("/events/" + eventoId + "/reservations")
                .header("Authorization", "Bearer " + tAsis)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":" + cantidad + "}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String reservaId = json.readTree(res).get("id").asText();
        var conf = mvc.perform(post("/reservations/" + reservaId + "/confirm")
                .header("Authorization", "Bearer " + tAsis))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(conf).get("entradas").get(0).get("codigo").asText();
    }

    private String checkInBody(String codigo) {
        return "{\"codigo\":\"" + codigo + "\"}";
    }

    @Test
    @DisplayName("CP05: check-in de una entrada ya usada devuelve 409 ENTRADA_YA_USADA con usadaEn")
    void cp05() throws Exception {
        String tOrga = token("orga@eventpass.dev", "Orga123!");
        UUID eventoId = crearPublicar(tOrga, 10);
        String codigo = reservarConfirmar(token("asistente1@eventpass.dev", "Asis123!"), eventoId, 1);

        mvc.perform(post("/events/" + eventoId + "/check-ins").header("Authorization", "Bearer " + tOrga)
                .contentType(MediaType.APPLICATION_JSON).content(checkInBody(codigo)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultado").value("VALIDA"));

        mvc.perform(post("/events/" + eventoId + "/check-ins").header("Authorization", "Bearer " + tOrga)
                .contentType(MediaType.APPLICATION_JSON).content(checkInBody(codigo)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ENTRADA_YA_USADA"))
            .andExpect(jsonPath("$.usadaEn").exists());
    }

    @Test
    @DisplayName("Concurrencia check-in: 10 validaciones simultáneas del mismo código -> exactamente 1 VALIDA")
    void concurrenciaCheckIn() throws Exception {
        String tOrga = token("orga@eventpass.dev", "Orga123!");
        UUID eventoId = crearPublicar(tOrga, 10);
        String codigo = reservarConfirmar(token("asistente2@eventpass.dev", "Asis123!"), eventoId, 1);

        int n = 10;
        ExecutorService pool = Executors.newFixedThreadPool(10);
        List<Callable<Integer>> tareas = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            tareas.add(() -> mvc.perform(post("/events/" + eventoId + "/check-ins")
                    .header("Authorization", "Bearer " + tOrga)
                    .contentType(MediaType.APPLICATION_JSON).content(checkInBody(codigo)))
                .andReturn().getResponse().getStatus());
        }
        List<Future<Integer>> rs = pool.invokeAll(tareas);
        pool.shutdown();

        long validas = 0;
        for (Future<Integer> f : rs) {
            if (f.get() == 200) {
                validas++;
            }
        }
        org.assertj.core.api.Assertions.assertThat(validas).isEqualTo(1);
    }

    @Test
    @DisplayName("Check-in de código inexistente devuelve 404 ENTRADA_INEXISTENTE")
    void inexistente() throws Exception {
        String tOrga = token("orga@eventpass.dev", "Orga123!");
        UUID eventoId = crearPublicar(tOrga, 5);
        mvc.perform(post("/events/" + eventoId + "/check-ins").header("Authorization", "Bearer " + tOrga)
                .contentType(MediaType.APPLICATION_JSON).content(checkInBody("EVP-XXXX-XXXX-XXXX")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ENTRADA_INEXISTENTE"));
    }

    @Test
    @DisplayName("Check-in de una entrada en otro evento devuelve 422 ENTRADA_OTRO_EVENTO")
    void otroEvento() throws Exception {
        String tOrga = token("orga@eventpass.dev", "Orga123!");
        UUID eventoA = crearPublicar(tOrga, 5);
        UUID eventoB = crearPublicar(tOrga, 5);
        String codigoA = reservarConfirmar(token("asistente3@eventpass.dev", "Asis123!"), eventoA, 1);

        mvc.perform(post("/events/" + eventoB + "/check-ins").header("Authorization", "Bearer " + tOrga)
                .contentType(MediaType.APPLICATION_JSON).content(checkInBody(codigoA)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("ENTRADA_OTRO_EVENTO"));
    }

    @Test
    @DisplayName("Stats del evento reflejan vendidas, retenidas e ingresadas")
    void stats() throws Exception {
        String tOrga = token("orga@eventpass.dev", "Orga123!");
        UUID eventoId = crearPublicar(tOrga, 10);
        String tAsis = token("asistente4@eventpass.dev", "Asis123!");

        // 2 confirmadas (vendidas), 1 pendiente (retenida), 1 check-in (ingresada)
        String codigo = reservarConfirmar(tAsis, eventoId, 2);
        mvc.perform(post("/events/" + eventoId + "/reservations").header("Authorization", "Bearer " + tAsis)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":1}"))
            .andExpect(status().isCreated());
        mvc.perform(post("/events/" + eventoId + "/check-ins").header("Authorization", "Bearer " + tOrga)
                .contentType(MediaType.APPLICATION_JSON).content(checkInBody(codigo)))
            .andExpect(status().isOk());

        var stats = mvc.perform(get("/events/" + eventoId + "/stats").header("Authorization", "Bearer " + tOrga))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode s = json.readTree(stats);
        org.assertj.core.api.Assertions.assertThat(s.get("cupoTotal").asInt()).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(s.get("vendidas").asInt()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(s.get("retenidas").asInt()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(s.get("ingresadas").asInt()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(s.get("disponibles").asInt()).isEqualTo(7);
    }

    @Test
    @DisplayName("Stats sin token devuelve 401 (no son públicas)")
    void statsSinToken() throws Exception {
        String tOrga = token("orga@eventpass.dev", "Orga123!");
        UUID eventoId = crearPublicar(tOrga, 5);
        mvc.perform(get("/events/" + eventoId + "/stats")).andExpect(status().isUnauthorized());
    }
}
