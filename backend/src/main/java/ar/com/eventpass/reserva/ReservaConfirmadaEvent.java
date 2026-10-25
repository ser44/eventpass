package ar.com.eventpass.reserva;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Evento de dominio: una reserva se confirmó (dispara la notificación por outbox). */
public record ReservaConfirmadaEvent(
    UUID reservaId,
    String email,
    String nombre,
    String eventoTitulo,
    Instant eventoFecha,
    String eventoLugar,
    List<String> codigos
) {
}
