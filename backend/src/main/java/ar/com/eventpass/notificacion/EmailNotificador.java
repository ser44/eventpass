package ar.com.eventpass.notificacion;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;

/** Canal por email (SMTP -> Mailpit en dev). Activo por defecto (app.notificacion.canal=email). */
@Component
@ConditionalOnProperty(name = "app.notificacion.canal", havingValue = "email", matchIfMissing = true)
public class EmailNotificador implements CanalNotificacion {

    private static final String REMITENTE = "no-reply@eventpass.dev";

    private final JavaMailSender mailSender;

    public EmailNotificador(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void enviar(NotificacionReservaPayload p) throws Exception {
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
        helper.setFrom(REMITENTE);
        helper.setTo(p.email());
        helper.setSubject(asunto(p));
        helper.setText(cuerpo(p), true);
        mailSender.send(mime);
    }

    private String asunto(NotificacionReservaPayload p) {
        return "RESERVA_CANCELADA".equals(p.tipo())
            ? "Tu reserva fue cancelada: " + p.eventoTitulo()
            : "Tus entradas para " + p.eventoTitulo();
    }

    private String cuerpo(NotificacionReservaPayload p) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>").append(escape(p.eventoTitulo())).append("</h2>");
        sb.append("<p>Hola ").append(escape(p.nombre())).append(",</p>");
        if ("RESERVA_CANCELADA".equals(p.tipo())) {
            sb.append("<p>Tu reserva fue cancelada. El cupo quedó liberado.</p>");
        } else {
            sb.append("<p>¡Tu reserva está confirmada! Estas son tus entradas:</p><ul>");
            if (p.codigos() != null) {
                for (String c : p.codigos()) {
                    sb.append("<li><code>").append(escape(c)).append("</code></li>");
                }
            }
            sb.append("</ul><p>Mostrá el código (o su QR) en el ingreso.</p>");
        }
        sb.append("<p><small>Lugar: ").append(escape(p.eventoLugar())).append("</small></p>");
        return sb.toString();
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
