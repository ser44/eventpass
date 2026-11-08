package ar.com.eventpass.evento;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.com.eventpass.comun.PaginaDto;
import ar.com.eventpass.evento.dto.ActualizarEventoRequest;
import ar.com.eventpass.evento.dto.CrearEventoRequest;
import ar.com.eventpass.evento.dto.EventoDto;
import ar.com.eventpass.evento.dto.EventoResumenDto;
import ar.com.eventpass.evento.dto.StatsDto;
import jakarta.validation.Valid;

@RestController
@Profile("!worker")
public class EventoController {

    private final EventoService service;
    private final EstadisticasService estadisticas;

    public EventoController(EventoService service, EstadisticasService estadisticas) {
        this.service = service;
        this.estadisticas = estadisticas;
    }

    // ---------- Público ----------

    @GetMapping("/events")
    public PaginaDto<EventoResumenDto> catalogo(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return service.listarCatalogo(q, from, to, pageable);
    }

    @GetMapping("/events/{id}")
    public EventoDto detalle(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID usuarioId = jwt == null ? null : UUID.fromString(jwt.getSubject());
        boolean esAdmin = esAdmin(jwt);
        return service.buscarDetalle(id, usuarioId, esAdmin);
    }

    // ---------- Organizador ----------

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public EventoDto crear(@Valid @RequestBody CrearEventoRequest req, @AuthenticationPrincipal Jwt jwt) {
        return service.crear(idDe(jwt), req);
    }

    @PutMapping("/events/{id}")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public EventoDto actualizar(@PathVariable UUID id, @Valid @RequestBody ActualizarEventoRequest req,
                                @AuthenticationPrincipal Jwt jwt) {
        return service.actualizar(id, idDe(jwt), false, req);
    }

    @PostMapping("/events/{id}/publish")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public EventoDto publicar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.publicar(id, idDe(jwt), false);
    }

    @PostMapping("/events/{id}/cancel")
    @PreAuthorize("hasAnyRole('ORGANIZADOR', 'ADMIN')")
    public EventoDto cancelar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.cancelar(id, idDe(jwt), esAdmin(jwt));
    }

    @GetMapping("/me/events")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public List<EventoResumenDto> misEventos(@AuthenticationPrincipal Jwt jwt) {
        return service.listarMisEventos(idDe(jwt));
    }

    @GetMapping("/events/{id}/stats")
    @PreAuthorize("hasAnyRole('ORGANIZADOR', 'ADMIN')")
    public StatsDto stats(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return estadisticas.de(id, idDe(jwt), esAdmin(jwt));
    }

    private static UUID idDe(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static boolean esAdmin(Jwt jwt) {
        return jwt != null && "ADMIN".equals(jwt.getClaimAsString("rol"));
    }
}
