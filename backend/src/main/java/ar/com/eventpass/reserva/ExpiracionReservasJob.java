package ar.com.eventpass.reserva;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.com.eventpass.evento.EventoRepository;

/**
 * CP04: cada 15 s pasa a EXPIRADA las reservas PENDIENTE vencidas y restituye su
 * cupo en la misma transacción. Corre en el worker (el @Scheduled solo dispara
 * si hay scheduling activo, perfil worker).
 */
@Component
public class ExpiracionReservasJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiracionReservasJob.class);
    private static final int TAMANIO_LOTE = 50;

    private final ReservaRepository reservaRepo;
    private final EventoRepository eventoRepo;
    private final Clock clock;

    public ExpiracionReservasJob(ReservaRepository reservaRepo, EventoRepository eventoRepo, Clock clock) {
        this.reservaRepo = reservaRepo;
        this.eventoRepo = eventoRepo;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public int expirarVencidas() {
        List<Reserva> vencidas = reservaRepo.tomarVencidas(Instant.now(clock), TAMANIO_LOTE);
        for (Reserva r : vencidas) {
            r.expirar();
            eventoRepo.restituirCupo(r.getEventoId(), r.getCantidad());
        }
        reservaRepo.saveAll(vencidas);
        if (!vencidas.isEmpty()) {
            log.info("Expiradas {} reservas vencidas; cupo restituido", vencidas.size());
        }
        return vencidas.size();
    }
}
