package ar.com.eventpass.entrada.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckInRequest(
    @NotBlank String codigo
) {
}
