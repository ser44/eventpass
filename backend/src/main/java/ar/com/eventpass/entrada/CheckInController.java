package ar.com.eventpass.entrada;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ar.com.eventpass.entrada.dto.CheckInRequest;
import ar.com.eventpass.entrada.dto.CheckInResultadoDto;
import jakarta.validation.Valid;

@RestController
@Profile("!worker")
public class CheckInController {

    private final CheckInService service;

    public CheckInController(CheckInService service) {
        this.service = service;
    }

    @PostMapping("/events/{id}/check-ins")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public CheckInResultadoDto checkIn(@PathVariable UUID id,
                                       @Valid @RequestBody CheckInRequest req,
                                       @AuthenticationPrincipal Jwt jwt) {
        return service.validar(id, req.codigo().trim(), UUID.fromString(jwt.getSubject()));
    }
}
