package ar.com.eventpass.evento;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.eventpass.comun.ConflictoDeVersionException;
import ar.com.eventpass.comun.PaginaDto;
import ar.com.eventpass.comun.RecursoNoEncontradoException;
import ar.com.eventpass.evento.dto.ActualizarEventoRequest;
import ar.com.eventpass.evento.dto.CrearEventoRequest;
import ar.com.eventpass.evento.dto.EventoDto;
import ar.com.eventpass.evento.dto.EventoResumenDto;

@Service
public class EventoService {

    private final EventoRepository repo;
    private final EventoMapper mapper;
    private final Clock clock;

    public EventoService(EventoRepository repo, EventoMapper mapper, Clock clock) {
        this.repo = repo;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public EventoDto crear(UUID organizadorId, CrearEventoRequest req) {
        Evento e = new Evento(organizadorId, req.titulo(), req.descripcion(), req.lugar(),
            req.fechaInicio(), req.cupoTotal(), req.imagenUrl());
        return mapper.aDto(repo.save(e));
    }

    @Transactional
    public EventoDto actualizar(UUID id, UUID usuarioId, boolean esAdmin, ActualizarEventoRequest req) {
        Evento e = cargarPropio(id, usuarioId, esAdmin);
        if (req.version() != e.getVersion()) {
            throw new ConflictoDeVersionException();
        }
        e.actualizarDatos(req.titulo(), req.descripcion(), req.lugar(), req.fechaInicio(), req.imagenUrl());
        e.cambiarCupoTotal(req.cupoTotal());
        return mapper.aDto(repo.save(e));
    }

    @Transactional
    public EventoDto publicar(UUID id, UUID usuarioId, boolean esAdmin) {
        Evento e = cargarPropio(id, usuarioId, esAdmin);
        e.publicar(clock);
        return mapper.aDto(repo.save(e));
    }

    @Transactional
    public EventoDto cancelar(UUID id, UUID usuarioId, boolean esAdmin) {
        Evento e = cargarPropio(id, usuarioId, esAdmin);
        e.cancelar();
        // La anulación de entradas y la notificación se agregan en Fases 3/4.
        return mapper.aDto(repo.save(e));
    }

    @Transactional(readOnly = true)
    public EventoDto buscarDetalle(UUID id, UUID usuarioIdOrNull, boolean esAdmin) {
        Evento e = repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("evento " + id));
        boolean visible = e.getEstado() == EstadoEvento.PUBLICADO
            || esAdmin
            || (usuarioIdOrNull != null && e.esDe(usuarioIdOrNull));
        if (!visible) {
            throw new RecursoNoEncontradoException("evento " + id);
        }
        return mapper.aDto(e);
    }

    private static final Instant SIN_TOPE = Instant.parse("9999-01-01T00:00:00Z");

    @Transactional(readOnly = true)
    public PaginaDto<EventoResumenDto> listarCatalogo(String q, Instant desde, Instant hasta, Pageable pageable) {
        String filtroQ = (q == null) ? "" : q.trim();
        Instant desdeEf = (desde != null) ? desde : Instant.EPOCH;
        Instant hastaEf = (hasta != null) ? hasta : SIN_TOPE;
        Page<Evento> page = repo.buscarPublicados(Instant.now(clock), filtroQ, desdeEf, hastaEf, pageable);
        List<EventoResumenDto> contenido = page.getContent().stream().map(mapper::aResumen).toList();
        return PaginaDto.desde(page, contenido);
    }

    @Transactional(readOnly = true)
    public List<EventoResumenDto> listarMisEventos(UUID organizadorId) {
        return repo.findByOrganizadorIdOrderByCreadoEnDesc(organizadorId).stream()
            .map(mapper::aResumen).toList();
    }

    /** Carga el evento validando propiedad; a un tercero se le responde 404 (no revelar existencia). */
    private Evento cargarPropio(UUID id, UUID usuarioId, boolean esAdmin) {
        Evento e = repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("evento " + id));
        if (!esAdmin && !e.esDe(usuarioId)) {
            throw new RecursoNoEncontradoException("evento " + id);
        }
        return e;
    }
}
