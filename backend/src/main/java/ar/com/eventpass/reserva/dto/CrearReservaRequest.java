package ar.com.eventpass.reserva.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** RN2: una reserva admite entre 1 y 4 entradas. */
public record CrearReservaRequest(
    @Min(1) @Max(4) int cantidad
) {
}
