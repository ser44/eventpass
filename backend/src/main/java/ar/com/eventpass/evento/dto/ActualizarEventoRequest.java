package ar.com.eventpass.evento.dto;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarEventoRequest(
    @NotBlank @Size(max = 160) String titulo,
    @Size(max = 4000) String descripcion,
    @NotBlank @Size(max = 200) String lugar,
    @NotNull @Future Instant fechaInicio,
    @Min(1) int cupoTotal,
    @Size(max = 500) String imagenUrl,
    @NotNull Long version
) {
}
