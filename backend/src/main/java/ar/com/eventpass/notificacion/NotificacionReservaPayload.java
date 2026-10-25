package ar.com.eventpass.notificacion;

import java.time.Instant;
import java.util.List;

/** Datos autocontenidos para enviar una notificación (el worker no depende de entidades). */
public record NotificacionReservaPayload(
    String tipo,
    String email,
    String nombre,
    String eventoTitulo,
    Instant eventoFecha,
    String eventoLugar,
    List<String> codigos
) {
}
