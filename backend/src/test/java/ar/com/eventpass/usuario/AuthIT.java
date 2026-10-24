package ar.com.eventpass.usuario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ar.com.eventpass.IntegracionBaseIT;
import ar.com.eventpass.usuario.dto.LoginRequest;
import ar.com.eventpass.usuario.dto.RegistroRequest;

class AuthIT extends IntegracionBaseIT {

    private String body(Object o) throws Exception {
        return json.writeValueAsString(o);
    }

    @Test
    @DisplayName("Registro crea un ASISTENTE y el login devuelve token")
    void registroYLoginOk() throws Exception {
        var reg = new RegistroRequest("nuevo@test.dev", "Nuevo Usuario", "Password1");
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body(reg)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rol").value("ASISTENTE"))
            .andExpect(jsonPath("$.email").value("nuevo@test.dev"));

        var login = new LoginRequest("nuevo@test.dev", "Password1");
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.usuario.email").value("nuevo@test.dev"));
    }

    @Test
    @DisplayName("Login con contraseña incorrecta devuelve 401 CREDENCIALES_INVALIDAS")
    void loginFallido() throws Exception {
        var reg = new RegistroRequest("malpass@test.dev", "Mal Pass", "Password1");
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body(reg)))
            .andExpect(status().isCreated());

        var login = new LoginRequest("malpass@test.dev", "otraClave9");
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(login)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    @DisplayName("Registrar un email ya existente devuelve 409 EMAIL_DUPLICADO")
    void registroEmailDuplicado() throws Exception {
        var reg = new RegistroRequest("dup@test.dev", "Duplicado", "Password1");
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body(reg)))
            .andExpect(status().isCreated());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body(reg)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("EMAIL_DUPLICADO"));
    }

    @Test
    @DisplayName("Registro con datos inválidos devuelve 400 VALIDACION")
    void registroInvalido() throws Exception {
        var reg = new RegistroRequest("no-es-email", "", "corta");
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body(reg)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDACION"))
            .andExpect(jsonPath("$.errores").isArray());
    }

    @Test
    @DisplayName("GET /auth/me sin token devuelve 401")
    void meSinToken() throws Exception {
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /auth/me con token devuelve el usuario autenticado")
    void meConToken() throws Exception {
        var reg = new RegistroRequest("me@test.dev", "Yo Mismo", "Password1");
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body(reg)))
            .andExpect(status().isCreated());

        var login = new LoginRequest("me@test.dev", "Password1");
        var respuesta = mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body(login)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String token = json.readTree(respuesta).get("token").asText();

        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("me@test.dev"));
    }
}
