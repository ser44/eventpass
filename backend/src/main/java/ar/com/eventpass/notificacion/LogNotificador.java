package ar.com.eventpass.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Canal que solo loguea la notificación (útil sin SMTP). Se activa con app.notificacion.canal=log. */
@Component
@ConditionalOnProperty(name = "app.notificacion.canal", havingValue = "log")
public class LogNotificador implements CanalNotificacion {

    private static final Logger log = LoggerFactory.getLogger(LogNotificador.class);

    @Override
    public void enviar(NotificacionReservaPayload p) {
        log.info("Notificacion [{}] a {}: evento='{}' codigos={}",
            p.tipo(), p.email(), p.eventoTitulo(), p.codigos());
    }
}
