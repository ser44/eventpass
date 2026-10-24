package ar.com.eventpass.comun;

import org.springframework.http.HttpStatus;

/** Se intentó una transición de estado no permitida (patrón State). */
public class TransicionInvalidaException extends DominioException {

    public TransicionInvalidaException(String mensaje) {
        super(mensaje);
    }

    @Override
    public String getCode() {
        return "TRANSICION_INVALIDA";
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
