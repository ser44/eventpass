package ar.com.eventpass.comun;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.com.eventpass.evento.Evento;
import ar.com.eventpass.evento.EventoRepository;
import ar.com.eventpass.notificacion.EstadoOutbox;
import ar.com.eventpass.notificacion.MensajeOutboxRepository;
import ar.com.eventpass.reserva.EstadoReserva;
import ar.com.eventpass.reserva.ReservaRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;

/**
 * Métricas de negocio (§9.2, nombres exactos). Los contadores se incrementan
 * desde los servicios de dominio; los gauges se registran acá y Prometheus
 * los lee en cada scrape. El de cupo por evento se refresca cada 15 s porque
 * el conjunto de eventos publicados cambia con el tiempo (MultiGauge).
 */
@Component
public class MetricasNegocio {

    private final MeterRegistry registry;
    private final EventoRepository eventoRepo;
    private final Clock clock;
    private final MultiGauge cupoDisponible;

    public MetricasNegocio(MeterRegistry registry, EventoRepository eventoRepo,
                           MensajeOutboxRepository outboxRepo, ReservaRepository reservaRepo, Clock clock) {
        this.registry = registry;
        this.eventoRepo = eventoRepo;
        this.clock = clock;

        Gauge.builder("eventpass_outbox_pendientes", outboxRepo,
                r -> r.countByEstado(EstadoOutbox.PENDIENTE))
            .register(registry);
        Gauge.builder("eventpass_reservas_vencidas_sin_procesar", reservaRepo,
                r -> r.countByEstadoAndExpiraEnBefore(EstadoReserva.PENDIENTE, Instant.now(clock)))
            .register(registry);

        this.cupoDisponible = MultiGauge.builder("eventpass_cupo_disponible").register(registry);
        actualizarCupoDisponible();
    }

    public void reserva(String resultado) {
        registry.counter("eventpass_reservas_total", "resultado", resultado).increment();
    }

    public void reservasExpiradas(long cantidad) {
        if (cantidad > 0) {
            registry.counter("eventpass_reservas_expiradas_total").increment(cantidad);
        }
    }

    public void checkIn(String resultado) {
        registry.counter("eventpass_checkin_total", "resultado", resultado).increment();
    }

    /** Solo eventos publicados y futuros (§9.2). Corre en el worker (perfil con scheduling activo). */
    @Scheduled(fixedDelay = 15000)
    public void actualizarCupoDisponible() {
        List<Evento> publicados = eventoRepo.buscarTodosPublicadosFuturos(Instant.now(clock));
        List<MultiGauge.Row<?>> filas = publicados.stream()
            .<MultiGauge.Row<?>>map(e -> MultiGauge.Row.of(Tags.of("evento", e.getTitulo()), (double) e.getCupoDisponible()))
            .toList();
        cupoDisponible.register(filas, true);
    }
}
