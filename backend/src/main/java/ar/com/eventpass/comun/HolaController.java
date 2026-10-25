package ar.com.eventpass.comun;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint mínimo de la Fase 0 para verificar la cadena
 * navegador → nginx → api. Se reemplazará por los módulos de negocio.
 */
@RestController
@Profile("!worker")
public class HolaController {

    @GetMapping("/hola")
    public Map<String, Object> hola() {
        return Map.of(
            "servicio", "eventpass-api",
            "mensaje", "Hola desde el backend",
            "hora", OffsetDateTime.now(Clock.systemDefaultZone()).toString()
        );
    }
}
