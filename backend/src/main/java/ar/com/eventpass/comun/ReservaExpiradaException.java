package ar.com.eventpass.comun;

import org.springframework.http.HttpStatus;

/** RN3: la reserva venció antes de confirmarse. */
public class ReservaExpiradaException extends DominioException {

    public ReservaExpiradaException() {
        super("La reserva venció. Volvé a intentarlo.");
    }

    @Override
    public String getCode() {
        return "RESERVA_EXPIRADA";
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.GONE;
    }
}
