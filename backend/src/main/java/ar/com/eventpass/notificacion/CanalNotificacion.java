package ar.com.eventpass.notificacion;

/** Strategy: canal por el que se envía una notificación (email, log, ...). */
public interface CanalNotificacion {

    void enviar(NotificacionReservaPayload payload) throws Exception;
}
