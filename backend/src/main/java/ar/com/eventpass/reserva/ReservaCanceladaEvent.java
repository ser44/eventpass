package ar.com.eventpass.reserva;

import java.time.Instant;
import java.util.UUID;

/** Evento de dominio: una reserva confirmada se canceló. */
public record ReservaCanceladaEvent(
    UUID reservaId,
    String email,
    String nombre,
    String eventoTitulo,
    Instant eventoFecha,
    String eventoLugar
) {
}
