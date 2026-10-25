package ar.com.eventpass.comun;

import org.springframework.http.HttpStatus;

/** RN1: el evento no está PUBLICADO o su fecha ya pasó. */
public class EventoNoReservableException extends DominioException {

    public EventoNoReservableException() {
        super("El evento no está disponible para reservar");
    }

    @Override
    public String getCode() {
        return "EVENTO_NO_RESERVABLE";
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
