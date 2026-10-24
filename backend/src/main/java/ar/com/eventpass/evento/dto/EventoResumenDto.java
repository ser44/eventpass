package ar.com.eventpass.evento.dto;

import java.time.Instant;
import java.util.UUID;

import ar.com.eventpass.evento.EstadoEvento;

/** Vista reducida para el catálogo y el panel del organizador. */
public record EventoResumenDto(
    UUID id,
    String titulo,
    String lugar,
    Instant fechaInicio,
    int cupoTotal,
    int cupoDisponible,
    EstadoEvento estado,
    String imagenUrl
) {
}
