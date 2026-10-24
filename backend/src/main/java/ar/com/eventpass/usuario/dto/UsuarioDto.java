package ar.com.eventpass.usuario.dto;

import java.time.Instant;
import java.util.UUID;

import ar.com.eventpass.usuario.Rol;
import ar.com.eventpass.usuario.Usuario;

/** Vista pública de un usuario (sin hash de contraseña). */
public record UsuarioDto(
    UUID id,
    String email,
    String nombre,
    Rol rol,
    boolean activo,
    Instant creadoEn
) {
    public static UsuarioDto desde(Usuario u) {
        return new UsuarioDto(u.getId(), u.getEmail(), u.getNombre(), u.getRol(), u.isActivo(), u.getCreadoEn());
    }
}
