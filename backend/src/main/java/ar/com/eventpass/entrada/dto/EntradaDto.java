package ar.com.eventpass.entrada.dto;

import java.time.Instant;
import java.util.UUID;

import ar.com.eventpass.entrada.EstadoEntrada;

/** Una entrada para "Mis entradas": incluye el código (para el QR) y datos del evento. */
public record EntradaDto(
    UUID id,
    String codigo,
    EstadoEntrada estado,
    Instant usadaEn,
    UUID reservaId,
    UUID eventoId,
    String eventoTitulo,
    Instant eventoFecha,
    String eventoLugar
) {
}
