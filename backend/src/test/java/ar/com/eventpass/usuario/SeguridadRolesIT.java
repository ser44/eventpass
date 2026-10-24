package ar.com.eventpass.usuario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ar.com.eventpass.IntegracionBaseIT;
import ar.com.eventpass.usuario.dto.LoginRequest;
import ar.com.eventpass.usuario.dto.RegistroRequest;

/**
 * Enforcement de roles. CP06 (ASISTENTE -> POST /events -> 403) se completa en
 * la Fase 2, cuando exista el endpoint de eventos; acá se verifica el mismo
 * mecanismo contra un endpoint de ADMIN.
 */
class SeguridadRolesIT extends IntegracionBaseIT {

    private String token(String email, String password) throws Exception {
        var login = new LoginRequest(email, password);
        var resp = mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("token").asText();
    }

    @Test
    @DisplayName("ASISTENTE no puede acceder a un endpoint de ADMIN (403)")
    void asistenteNoAccedeAdmin() throws Exception {
        var reg = new RegistroRequest("asis-rol@test.dev", "Asistente Rol", "Password1");
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(reg)))
            .andExpect(status().isCreated());

        String token = token("asis-rol@test.dev", "Password1");
        mvc.perform(get("/admin/users").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN (semilla) sí puede listar usuarios")
    void adminAccedeAdmin() throws Exception {
        String token = token("admin@eventpass.dev", "Admin123!");
        mvc.perform(get("/admin/users").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sin token, el endpoint de ADMIN devuelve 401")
    void sinTokenAdmin() throws Exception {
        mvc.perform(get("/admin/users")).andExpect(status().isUnauthorized());
    }
}
