package ar.com.eventpass.entrada;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.eventpass.comun.EntradaDeOtroEventoException;
import ar.com.eventpass.comun.EntradaInexistenteException;
import ar.com.eventpass.comun.RecursoNoEncontradoException;
import ar.com.eventpass.entrada.dto.CheckInResultadoDto;
import ar.com.eventpass.evento.Evento;
import ar.com.eventpass.evento.EventoRepository;
import ar.com.eventpass.reserva.Reserva;
import ar.com.eventpass.reserva.ReservaRepository;
import ar.com.eventpass.usuario.UsuarioService;

/**
 * Valida el ingreso de una entrada por su código (RN6). Usa bloqueo pesimista
 * para que check-ins simultáneos del mismo código se serialicen (solo uno VALIDA).
 * Cruza de módulo (evento, reserva, usuario) a propósito, vía repos/servicios.
 */
@Service
public class CheckInService {

    private final EntradaRepository entradaRepo;
    private final EventoRepository eventoRepo;
    private final ReservaRepository reservaRepo;
    private final UsuarioService usuarioService;
    private final Clock clock;

    public CheckInService(EntradaRepository entradaRepo, EventoRepository eventoRepo,
                          ReservaRepository reservaRepo, UsuarioService usuarioService, Clock clock) {
        this.entradaRepo = entradaRepo;
        this.eventoRepo = eventoRepo;
        this.reservaRepo = reservaRepo;
        this.usuarioService = usuarioService;
        this.clock = clock;
    }

    @Transactional
    public CheckInResultadoDto validar(UUID eventoId, String codigo, UUID staffId) {
        // El check-in lo hace el organizador dueño; a un tercero se le responde 404.
        Evento evento = eventoRepo.findById(eventoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("evento " + eventoId));
        if (!evento.esDe(staffId)) {
            throw new RecursoNoEncontradoException("evento " + eventoId);
        }

        Entrada entrada = entradaRepo.findByCodigoBloqueando(codigo)
            .orElseThrow(EntradaInexistenteException::new);
        if (!entrada.getEventoId().equals(eventoId)) {
            throw new EntradaDeOtroEventoException();
        }

        entrada.validarIngreso(clock, staffId); // lanza si ya usada o anulada
        entradaRepo.save(entrada);

        return new CheckInResultadoDto("VALIDA", titular(entrada), entrada.getId());
    }

    private String titular(Entrada entrada) {
        return reservaRepo.findById(entrada.getReservaId())
            .map(Reserva::getUsuarioId)
            .map(id -> usuarioService.buscarPorId(id).nombre())
            .orElse(null);
    }
}
