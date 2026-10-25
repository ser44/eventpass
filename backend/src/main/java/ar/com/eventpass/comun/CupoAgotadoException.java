package ar.com.eventpass.comun;

import java.util.Map;

import org.springframework.http.HttpStatus;

/** RN4/venta: no quedan lugares suficientes. Informa el cupo disponible actual. */
public class CupoAgotadoException extends DominioException {

    private final int cupoDisponible;

    public CupoAgotadoException(int cupoDisponible) {
        super("No quedan entradas suficientes");
        this.cupoDisponible = cupoDisponible;
    }

    @Override
    public String getCode() {
        return "CUPO_AGOTADO";
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public Map<String, Object> getPropiedades() {
        return Map.of("cupoDisponible", cupoDisponible);
    }
}
