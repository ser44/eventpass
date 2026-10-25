package ar.com.eventpass.reserva.dto;

import java.util.List;
import java.util.UUID;

import ar.com.eventpass.entrada.dto.EntradaDto;
import ar.com.eventpass.reserva.EstadoReserva;

public record ReservaConfirmadaDto(
    UUID id,
    UUID eventoId,
    String eventoTitulo,
    EstadoReserva estado,
    List<EntradaDto> entradas
) {
}
