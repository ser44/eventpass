package ar.com.eventpass.usuario;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.com.eventpass.usuario.dto.LoginRequest;
import ar.com.eventpass.usuario.dto.LoginResponse;
import ar.com.eventpass.usuario.dto.RegistroRequest;
import ar.com.eventpass.usuario.dto.UsuarioDto;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Profile("!worker")
public class AuthController {

    private final UsuarioService service;

    public AuthController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto register(@Valid @RequestBody RegistroRequest req) {
        return service.registrar(req);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return service.autenticar(req);
    }

    @GetMapping("/me")
    public UsuarioDto me(@AuthenticationPrincipal Jwt jwt) {
        return service.buscarPorId(UUID.fromString(jwt.getSubject()));
    }
}
