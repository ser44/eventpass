package ar.com.eventpass.notificacion;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Despachador de la outbox (corre en el worker). Cada 5 s toma un lote con
 * FOR UPDATE SKIP LOCKED, lo envía por el canal elegido (Strategy) y marca
 * ENVIADO o reprograma con backoff; tras 5 intentos queda FALLIDO.
 * El @Scheduled solo dispara si hay scheduling activo (perfil worker).
 */
@Component
public class DespachadorOutbox {

    private static final Logger log = LoggerFactory.getLogger(DespachadorOutbox.class);
    private static final int MAX_INTENTOS = 5;
    private static final int BACKOFF_SEGUNDOS = 10;
    private static final int TAMANIO_LOTE = 20;

    private final MensajeOutboxRepository repo;
    private final CanalNotificacion canal;
    private final ObjectMapper json;
    private final Clock clock;

    public DespachadorOutbox(MensajeOutboxRepository repo, CanalNotificacion canal,
                             ObjectMapper json, Clock clock) {
        this.repo = repo;
        this.canal = canal;
        this.json = json;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void despachar() {
        List<MensajeOutbox> lote = repo.tomarLote(Instant.now(clock), TAMANIO_LOTE);
        for (MensajeOutbox m : lote) {
            try {
                NotificacionReservaPayload payload =
                    json.readValue(m.getPayload(), NotificacionReservaPayload.class);
                canal.enviar(payload);
                m.marcarEnviado(clock);
            } catch (Exception ex) {
                m.registrarFallo(clock, ex.getMessage(), MAX_INTENTOS, BACKOFF_SEGUNDOS);
                if (m.getEstado() == EstadoOutbox.FALLIDO) {
                    log.error("Outbox {} FALLIDO tras {} intentos: {}", m.getId(), m.getIntentos(), ex.getMessage());
                } else {
                    log.warn("Outbox {} falló (intento {}): {}", m.getId(), m.getIntentos(), ex.getMessage());
                }
            }
        }
        repo.saveAll(lote);
    }
}
