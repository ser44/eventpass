package ar.com.eventpass.evento.dto;

/** Estadísticas de venta e ingreso de un evento. */
public record StatsDto(
    int cupoTotal,
    int disponibles,
    long retenidas,
    long vendidas,
    long canceladas,
    long ingresadas
) {
}
