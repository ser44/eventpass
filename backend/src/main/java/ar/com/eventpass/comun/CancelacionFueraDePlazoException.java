package ar.com.eventpass.comun;

import org.springframework.http.HttpStatus;

/** RN5: solo se puede cancelar hasta 48 h antes del evento. */
public class CancelacionFueraDePlazoException extends DominioException {

    public CancelacionFueraDePlazoException() {
        super("Ya no se puede cancelar (faltan menos de 48 h para el evento)");
    }

    @Override
    public String getCode() {
        return "CANCELACION_FUERA_DE_PLAZO";
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
