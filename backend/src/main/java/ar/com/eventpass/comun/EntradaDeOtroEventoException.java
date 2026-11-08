package ar.com.eventpass.comun;

import org.springframework.http.HttpStatus;

/** La entrada existe pero pertenece a otro evento (no al que se está validando). */
public class EntradaDeOtroEventoException extends DominioException {

    public EntradaDeOtroEventoException() {
        super("La entrada pertenece a otro evento");
    }

    @Override
    public String getCode() {
        return "ENTRADA_OTRO_EVENTO";
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
