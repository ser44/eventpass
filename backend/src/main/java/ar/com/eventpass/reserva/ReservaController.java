package ar.com.eventpass.reserva;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.com.eventpass.entrada.dto.EntradaDto;
import ar.com.eventpass.reserva.dto.CrearReservaRequest;
import ar.com.eventpass.reserva.dto.ReservaConfirmadaDto;
import ar.com.eventpass.reserva.dto.ReservaDto;
import jakarta.validation.Valid;

@RestController
@Profile("!worker")
public class ReservaController {

    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    @PostMapping("/events/{id}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaDto reservar(@PathVariable UUID id,
                               @Valid @RequestBody CrearReservaRequest req,
                               @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                               @AuthenticationPrincipal Jwt jwt) {
        return service.reservar(id, idDe(jwt), req.cantidad(), idempotencyKey);
    }

    @GetMapping("/reservations/{id}")
    public ReservaDto detalle(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.buscar(id, idDe(jwt));
    }

    @PostMapping("/reservations/{id}/confirm")
    public ReservaConfirmadaDto confirmar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.confirmar(id, idDe(jwt));
    }

    @DeleteMapping("/reservations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        service.cancelar(id, idDe(jwt));
    }

    @GetMapping("/me/tickets")
    public List<EntradaDto> misEntradas(@AuthenticationPrincipal Jwt jwt) {
        return service.misEntradas(idDe(jwt));
    }

    private static UUID idDe(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
