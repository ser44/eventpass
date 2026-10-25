package ar.com.eventpass.notificacion;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.com.eventpass.reserva.ReservaCanceladaEvent;
import ar.com.eventpass.reserva.ReservaConfirmadaEvent;

/**
 * Observer de eventos de dominio: ante una confirmación/cancelación inserta un
 * mensaje en la outbox DENTRO de la misma transacción (BEFORE_COMMIT). Así la
 * notificación nunca se pierde ni frena la reserva.
 */
@Component
public class OutboxListener {

    private final MensajeOutboxRepository repo;
    private final ObjectMapper json;

    public OutboxListener(MensajeOutboxRepository repo, ObjectMapper json) {
        this.repo = repo;
        this.json = json;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void alConfirmar(ReservaConfirmadaEvent e) {
        var payload = new NotificacionReservaPayload("RESERVA_CONFIRMADA", e.email(), e.nombre(),
            e.eventoTitulo(), e.eventoFecha(), e.eventoLugar(), e.codigos());
        guardar("RESERVA_CONFIRMADA", payload);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void alCancelar(ReservaCanceladaEvent e) {
        var payload = new NotificacionReservaPayload("RESERVA_CANCELADA", e.email(), e.nombre(),
            e.eventoTitulo(), e.eventoFecha(), e.eventoLugar(), List.of());
        guardar("RESERVA_CANCELADA", payload);
    }

    private void guardar(String tipo, NotificacionReservaPayload payload) {
        try {
            repo.save(MensajeOutbox.crear(tipo, json.writeValueAsString(payload)));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo serializar la notificación", ex);
        }
    }
}
