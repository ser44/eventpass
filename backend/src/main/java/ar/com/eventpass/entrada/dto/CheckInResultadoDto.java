package ar.com.eventpass.entrada.dto;

import java.util.UUID;

/** Resultado de un check-in exitoso (los rechazos van como error Problem Details). */
public record CheckInResultadoDto(
    String resultado,
    String titular,
    UUID entradaId
) {
}
