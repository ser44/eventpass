package ar.com.eventpass.comun;

import org.springframework.http.HttpStatus;

/** La entrada fue anulada (su reserva se canceló). */
public class EntradaAnuladaException extends DominioException {

    public EntradaAnuladaException() {
        super("La entrada está anulada");
    }

    @Override
    public String getCode() {
        return "ENTRADA_ANULADA";
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
