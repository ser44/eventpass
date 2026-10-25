package ar.com.eventpass.reserva.dto;

import java.time.Instant;
import java.util.UUID;

import ar.com.eventpass.reserva.EstadoReserva;

public record ReservaDto(
    UUID id,
    UUID eventoId,
    String eventoTitulo,
    int cantidad,
    EstadoReserva estado,
    Instant expiraEn,
    Instant creadaEn
) {
}
