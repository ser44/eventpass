package ar.com.eventpass.evento.dto;

import java.time.Instant;
import java.util.UUID;

import ar.com.eventpass.evento.EstadoEvento;

/** Detalle completo de un evento. */
public record EventoDto(
    UUID id,
    UUID organizadorId,
    String titulo,
    String descripcion,
    String lugar,
    Instant fechaInicio,
    int cupoTotal,
    int cupoDisponible,
    EstadoEvento estado,
    String imagenUrl,
    long version,
    Instant creadoEn,
    Instant actualizadoEn
) {
}
