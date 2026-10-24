package ar.com.eventpass.evento;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;

import ar.com.eventpass.IntegracionBaseIT;
import ar.com.eventpass.usuario.dto.LoginRequest;
import ar.com.eventpass.usuario.dto.RegistroRequest;

class EventoIT extends IntegracionBaseIT {

    @Autowired
    private EventoRepository eventoRepo;

    private String token(String email, String password) throws Exception {
        var login = new LoginRequest(email, password);
        var resp = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(login)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("token").asText();
    }

    private String tokenOrganizador() throws Exception {
        return token("orga@eventpass.dev", "Orga123!");
    }

    private String cuerpoEvento(String titulo, int cupo) throws Exception {
        Instant futuro = Instant.now().plus(30, ChronoUnit.DAYS);
        return """
            {"titulo":"%s","descripcion":"d","lugar":"Aula 1","fechaInicio":"%s","cupoTotal":%d}
            """.formatted(titulo, futuro.toString(), cupo);
    }

    private JsonNode crearEvento(String token, String titulo) throws Exception {
        var resp = mvc.perform(post("/events").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoEvento(titulo, 50)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(resp);
    }

    private boolean catalogoContiene(String titulo) throws Exception {
        var resp = mvc.perform(get("/events").param("size", "50"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var contenido = json.readTree(resp).get("contenido");
        for (JsonNode e : contenido) {
            if (titulo.equals(e.get("titulo").asText())) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("CP06: un ASISTENTE no puede crear eventos (403)")
    void cp06AsistenteNoCrea() throws Exception {
        var reg = new RegistroRequest("asis-ev@test.dev", "Asis Ev", "Password1");
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(reg))).andExpect(status().isCreated());
        String tAsis = token("asis-ev@test.dev", "Password1");

        mvc.perform(post("/events").header("Authorization", "Bearer " + tAsis)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpoEvento("Prohibido", 10)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El organizador crea un evento en BORRADOR")
    void organizadorCrea() throws Exception {
        JsonNode e = crearEvento(tokenOrganizador(), "Charla Angular " + UUID.randomUUID());
        org.assertj.core.api.Assertions.assertThat(e.get("estado").asText()).isEqualTo("BORRADOR");
    }

    @Test
    @DisplayName("RN1: un evento en BORRADOR no aparece en el catálogo; al publicarlo sí")
    void rn1BorradorNoEnCatalogoPublicadoSi() throws Exception {
        String t = tokenOrganizador();
        String titulo = "Recital " + UUID.randomUUID();
        JsonNode e = crearEvento(t, titulo);
        org.assertj.core.api.Assertions.assertThat(catalogoContiene(titulo)).isFalse();

        mvc.perform(post("/events/" + e.get("id").asText() + "/publish").header("Authorization", "Bearer " + t))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("PUBLICADO"));

        org.assertj.core.api.Assertions.assertThat(catalogoContiene(titulo)).isTrue();
    }

    @Test
    @DisplayName("RN1: un evento publicado pero con fecha pasada no aparece en el catálogo")
    void rn1PasadoNoEnCatalogo() throws Exception {
        // Un evento pasado no se puede crear por API (@Future); lo insertamos directo.
        var meResp = mvc.perform(get("/auth/me").header("Authorization", "Bearer " + tokenOrganizador()))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        UUID orgaId = UUID.fromString(json.readTree(meResp).get("id").asText());

        String titulo = "Evento Viejo " + UUID.randomUUID();
        Evento pasado = new Evento(orgaId, titulo, "d", "Aula", Instant.now().minus(2, ChronoUnit.DAYS), 10, null);
        ReflectionTestUtils.setField(pasado, "estado", EstadoEvento.PUBLICADO);
        eventoRepo.save(pasado);

        org.assertj.core.api.Assertions.assertThat(catalogoContiene(titulo)).isFalse();
    }

    @Test
    @DisplayName("El detalle de un evento inexistente devuelve 404")
    void detalleInexistente() throws Exception {
        mvc.perform(get("/events/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT con versión desactualizada devuelve 409 VERSION_DESACTUALIZADA")
    void conflictoDeVersion() throws Exception {
        String t = tokenOrganizador();
        JsonNode e = crearEvento(t, "Para Editar " + UUID.randomUUID());
        String body = """
            {"titulo":"Editado","descripcion":"d","lugar":"Aula 2","fechaInicio":"%s","cupoTotal":60,"version":999}
            """.formatted(Instant.now().plus(40, ChronoUnit.DAYS).toString());

        mvc.perform(put("/events/" + e.get("id").asText()).header("Authorization", "Bearer " + t)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("VERSION_DESACTUALIZADA"));
    }

    @Test
    @DisplayName("Editar el evento de otro organizador devuelve 404 (no revela existencia)")
    void eventoAjeno404() throws Exception {
        // Promovemos un asistente a ORGANIZADOR vía admin.
        var reg = new RegistroRequest("orga2@test.dev", "Orga Dos", "Password1");
        var regResp = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(reg)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String orga2Id = json.readTree(regResp).get("id").asText();

        String tAdmin = token("admin@eventpass.dev", "Admin123!");
        mvc.perform(patch("/admin/users/" + orga2Id).header("Authorization", "Bearer " + tAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"ORGANIZADOR\"}"))
            .andExpect(status().isOk());

        JsonNode e = crearEvento(tokenOrganizador(), "Mi Evento " + UUID.randomUUID());
        String tOrga2 = token("orga2@test.dev", "Password1");
        String body = """
            {"titulo":"Hackeado","descripcion":"d","lugar":"X","fechaInicio":"%s","cupoTotal":10,"version":0}
            """.formatted(Instant.now().plus(40, ChronoUnit.DAYS).toString());

        mvc.perform(put("/events/" + e.get("id").asText()).header("Authorization", "Bearer " + tOrga2)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNotFound());
    }
}
