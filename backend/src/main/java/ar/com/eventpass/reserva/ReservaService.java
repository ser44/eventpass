package ar.com.eventpass.reserva;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.com.eventpass.comun.CupoAgotadoException;
import ar.com.eventpass.comun.EventoNoReservableException;
import ar.com.eventpass.comun.RecursoNoEncontradoException;
import ar.com.eventpass.entrada.Entrada;
import ar.com.eventpass.entrada.EntradaRepository;
import ar.com.eventpass.entrada.dto.EntradaDto;
import ar.com.eventpass.evento.EstadoEvento;
import ar.com.eventpass.evento.Evento;
import ar.com.eventpass.evento.EventoRepository;
import ar.com.eventpass.idempotencia.IdempotenciaService;
import ar.com.eventpass.reserva.dto.ReservaConfirmadaDto;
import ar.com.eventpass.reserva.dto.ReservaDto;
import ar.com.eventpass.usuario.UsuarioService;
import ar.com.eventpass.usuario.dto.UsuarioDto;

/**
 * Casos de uso de reservas. Usa EventoRepository solo para el descuento/restitución
 * atómicos de cupo (§3.1) y EntradaRepository para emitir/anular entradas (§6.1);
 * ambos accesos cruzan de módulo a propósito y están documentados.
 */
@Service
public class ReservaService {

    private final ReservaRepository reservaRepo;
    private final EntradaRepository entradaRepo;
    private final EventoRepository eventoRepo;
    private final IdempotenciaService idempotencia;
    private final UsuarioService usuarioService;
    private final ApplicationEventPublisher eventos;
    private final ObjectMapper json;
    private final Clock clock;
    private final int ttlMinutos;
    private final int cancelacionHoras;

    public ReservaService(ReservaRepository reservaRepo, EntradaRepository entradaRepo,
                          EventoRepository eventoRepo, IdempotenciaService idempotencia,
                          UsuarioService usuarioService, ApplicationEventPublisher eventos,
                          ObjectMapper json, Clock clock,
                          @Value("${app.reserva.ttl-minutos}") int ttlMinutos,
                          @Value("${app.reserva.cancelacion-horas}") int cancelacionHoras) {
        this.reservaRepo = reservaRepo;
        this.entradaRepo = entradaRepo;
        this.eventoRepo = eventoRepo;
        this.idempotencia = idempotencia;
        this.usuarioService = usuarioService;
        this.eventos = eventos;
        this.json = json;
        this.clock = clock;
        this.ttlMinutos = ttlMinutos;
        this.cancelacionHoras = cancelacionHoras;
    }

    @Transactional
    public ReservaDto reservar(UUID eventoId, UUID usuarioId, int cantidad, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<String> guardada = idempotencia.buscarRespuesta(idempotencyKey, usuarioId);
            if (guardada.isPresent()) {
                return deserializar(guardada.get());
            }
        }
        ReservaDto dto = hacerReserva(eventoId, usuarioId, cantidad);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencia.guardar(idempotencyKey, usuarioId,
                "POST /events/{id}/reservations", 201, serializar(dto));
        }
        return dto;
    }

    private ReservaDto hacerReserva(UUID eventoId, UUID usuarioId, int cantidad) {
        // Decisión central: una sola sentencia atómica decide si hay cupo.
        int filas = eventoRepo.descontarCupo(eventoId, cantidad, Instant.now(clock));
        if (filas == 0) {
            Evento e = eventoRepo.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("evento " + eventoId));
            if (e.getEstado() != EstadoEvento.PUBLICADO || !e.getFechaInicio().isAfter(Instant.now(clock))) {
                throw new EventoNoReservableException();
            }
            throw new CupoAgotadoException(e.getCupoDisponible());
        }
        Reserva r = new Reserva(eventoId, usuarioId, cantidad, clock, ttlMinutos);
        reservaRepo.save(r);
        String titulo = eventoRepo.findById(eventoId).map(Evento::getTitulo).orElse(null);
        return new ReservaDto(r.getId(), eventoId, titulo, cantidad, r.getEstado(), r.getExpiraEn(), r.getCreadaEn());
    }

    @Transactional
    public ReservaConfirmadaDto confirmar(UUID reservaId, UUID usuarioId) {
        Reserva r = cargarPropia(reservaId, usuarioId);
        List<Entrada> entradas = r.confirmar(clock);
        entradaRepo.saveAll(entradas);
        reservaRepo.save(r);
        Evento e = eventoRepo.findById(r.getEventoId()).orElse(null);
        UsuarioDto u = usuarioService.buscarPorId(usuarioId);
        List<String> codigos = entradas.stream().map(Entrada::getCodigo).toList();
        // Observer + Outbox: el listener inserta el mensaje en esta misma transacción.
        eventos.publishEvent(new ReservaConfirmadaEvent(r.getId(), u.email(), u.nombre(),
            tituloDe(e), e == null ? null : e.getFechaInicio(), e == null ? null : e.getLugar(), codigos));
        List<EntradaDto> dtos = entradas.stream().map(en -> aEntradaDto(en, e)).toList();
        return new ReservaConfirmadaDto(r.getId(), r.getEventoId(), tituloDe(e), r.getEstado(), dtos);
    }

    @Transactional
    public void cancelar(UUID reservaId, UUID usuarioId) {
        Reserva r = cargarPropia(reservaId, usuarioId);
        Evento e = eventoRepo.findById(r.getEventoId())
            .orElseThrow(() -> new RecursoNoEncontradoException("evento " + r.getEventoId()));
        r.cancelar(clock, e.getFechaInicio(), cancelacionHoras);
        List<Entrada> entradas = entradaRepo.findByReservaId(r.getId());
        entradas.forEach(Entrada::anular);
        entradaRepo.saveAll(entradas);
        eventoRepo.restituirCupo(r.getEventoId(), r.getCantidad());
        reservaRepo.save(r);
        UsuarioDto u = usuarioService.buscarPorId(usuarioId);
        eventos.publishEvent(new ReservaCanceladaEvent(r.getId(), u.email(), u.nombre(),
            e.getTitulo(), e.getFechaInicio(), e.getLugar()));
    }

    @Transactional(readOnly = true)
    public ReservaDto buscar(UUID reservaId, UUID usuarioId) {
        Reserva r = cargarPropia(reservaId, usuarioId);
        String titulo = eventoRepo.findById(r.getEventoId()).map(Evento::getTitulo).orElse(null);
        return new ReservaDto(r.getId(), r.getEventoId(), titulo, r.getCantidad(), r.getEstado(),
            r.getExpiraEn(), r.getCreadaEn());
    }

    @Transactional(readOnly = true)
    public List<EntradaDto> misEntradas(UUID usuarioId) {
        List<Reserva> reservas = reservaRepo.findByUsuarioIdOrderByCreadaEnDesc(usuarioId);
        List<UUID> reservaIds = reservas.stream().map(Reserva::getId).toList();
        if (reservaIds.isEmpty()) {
            return List.of();
        }
        List<Entrada> entradas = entradaRepo.findByReservaIdIn(reservaIds);
        List<UUID> eventoIds = entradas.stream().map(Entrada::getEventoId).distinct().toList();
        Map<UUID, Evento> eventos = eventoRepo.findAllById(eventoIds).stream()
            .collect(Collectors.toMap(Evento::getId, Function.identity()));
        return entradas.stream().map(en -> aEntradaDto(en, eventos.get(en.getEventoId()))).toList();
    }

    /** Carga una reserva validando propiedad; a un tercero se le responde 404. */
    private Reserva cargarPropia(UUID reservaId, UUID usuarioId) {
        Reserva r = reservaRepo.findById(reservaId)
            .orElseThrow(() -> new RecursoNoEncontradoException("reserva " + reservaId));
        if (!r.esDe(usuarioId)) {
            throw new RecursoNoEncontradoException("reserva " + reservaId);
        }
        return r;
    }

    private EntradaDto aEntradaDto(Entrada en, Evento e) {
        return new EntradaDto(en.getId(), en.getCodigo(), en.getEstado(), en.getUsadaEn(),
            en.getReservaId(), en.getEventoId(), tituloDe(e),
            e == null ? null : e.getFechaInicio(), e == null ? null : e.getLugar());
    }

    private static String tituloDe(Evento e) {
        return e == null ? null : e.getTitulo();
    }

    private String serializar(ReservaDto dto) {
        try {
            return json.writeValueAsString(dto);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo serializar la reserva", ex);
        }
    }

    private ReservaDto deserializar(String s) {
        try {
            return json.readValue(s, ReservaDto.class);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo leer la respuesta idempotente", ex);
        }
    }
}
