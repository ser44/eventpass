package ar.com.eventpass.usuario.dto;

import ar.com.eventpass.usuario.Rol;

/** Cambios que un ADMIN puede aplicar sobre un usuario. Campos opcionales. */
public record ActualizarUsuarioRequest(
    Rol rol,
    Boolean activo
) {
}
