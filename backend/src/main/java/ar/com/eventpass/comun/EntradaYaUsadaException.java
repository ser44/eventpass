package ar.com.eventpass.comun;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;

/** RN6: la entrada ya fue utilizada. Informa cuándo (usadaEn). */
public class EntradaYaUsadaException extends DominioException {

    private final Instant usadaEn;

    public EntradaYaUsadaException(Instant usadaEn) {
        super("La entrada ya fue utilizada");
        this.usadaEn = usadaEn;
    }

    @Override
    public String getCode() {
        return "ENTRADA_YA_USADA";
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public Map<String, Object> getPropiedades() {
        return usadaEn == null ? Map.of() : Map.of("usadaEn", usadaEn);
    }
}
