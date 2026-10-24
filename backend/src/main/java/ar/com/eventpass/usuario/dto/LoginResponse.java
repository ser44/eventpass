package ar.com.eventpass.usuario.dto;

public record LoginResponse(
    String token,
    UsuarioDto usuario
) {
}
