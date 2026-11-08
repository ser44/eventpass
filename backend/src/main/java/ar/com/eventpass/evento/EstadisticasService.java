package ar.com.eventpass.evento;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.eventpass.comun.RecursoNoEncontradoException;
import ar.com.eventpass.entrada.EntradaRepository;
import ar.com.eventpass.entrada.EstadoEntrada;
import ar.com.eventpass.evento.dto.StatsDto;
import ar.com.eventpass.reserva.ReservaRepository;

/** Arma las estadísticas de un evento combinando datos de evento, reserva y entrada (cruce documentado). */
@Service
public class EstadisticasService {

    private final EventoRepository eventoRepo;
    private final ReservaRepository reservaRepo;
    private final EntradaRepository entradaRepo;

    public EstadisticasService(EventoRepository eventoRepo, ReservaRepository reservaRepo,
                               EntradaRepository entradaRepo) {
        this.eventoRepo = eventoRepo;
        this.reservaRepo = reservaRepo;
        this.entradaRepo = entradaRepo;
    }

    @Transactional(readOnly = true)
    public StatsDto de(UUID eventoId, UUID usuarioId, boolean esAdmin) {
        Evento e = eventoRepo.findById(eventoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("evento " + eventoId));
        if (!esAdmin && !e.esDe(usuarioId)) {
            throw new RecursoNoEncontradoException("evento " + eventoId);
        }
        long retenidas = reservaRepo.sumarRetenidas(eventoId);
        long vendidas = entradaRepo.countByEventoIdAndEstadoIn(eventoId,
            List.of(EstadoEntrada.VALIDA, EstadoEntrada.USADA));
        long canceladas = entradaRepo.countByEventoIdAndEstado(eventoId, EstadoEntrada.ANULADA);
        long ingresadas = entradaRepo.countByEventoIdAndEstado(eventoId, EstadoEntrada.USADA);
        return new StatsDto(e.getCupoTotal(), e.getCupoDisponible(), retenidas, vendidas, canceladas, ingresadas);
    }
}
