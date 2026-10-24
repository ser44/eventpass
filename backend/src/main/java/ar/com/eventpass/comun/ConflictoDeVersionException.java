package ar.com.eventpass.comun;

import org.springframework.http.HttpStatus;

/** La entidad fue modificada por otro desde que el cliente la leyó (bloqueo optimista). */
public class ConflictoDeVersionException extends DominioException {

    public ConflictoDeVersionException() {
        super("El recurso fue modificado por otra persona. Recargá e intentá de nuevo.");
    }

    @Override
    public String getCode() {
        return "VERSION_DESACTUALIZADA";
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
