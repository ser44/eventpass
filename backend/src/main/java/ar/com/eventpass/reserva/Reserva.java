package ar.com.eventpass.reserva;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import ar.com.eventpass.comun.CancelacionFueraDePlazoException;
import ar.com.eventpass.comun.ReservaExpiradaException;
import ar.com.eventpass.comun.TransicionInvalidaException;
import ar.com.eventpass.entrada.Entrada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Reserva con retención temporal de cupo. Dominio rico (patrón State): cada
 * transición valida el estado actual. El cupo se descuenta/restituye con un
 * UPDATE atómico en el servicio (no desde acá).
 */
@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    private UUID id;

    @Column(name = "evento_id", nullable = false)
    private UUID eventoId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false)
    private int cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado;

    @Column(name = "expira_en")
    private Instant expiraEn;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "confirmada_en")
    private Instant confirmadaEn;

    @Column(name = "cancelada_en")
    private Instant canceladaEn;

    protected Reserva() {
    }

    public Reserva(UUID eventoId, UUID usuarioId, int cantidad, Clock reloj, int ttlMinutos) {
        if (cantidad < 1 || cantidad > 4) {
            throw new IllegalArgumentException("La cantidad debe estar entre 1 y 4");
        }
        Instant ahora = Instant.now(reloj);
        this.id = UUID.randomUUID();
        this.eventoId = eventoId;
        this.usuarioId = usuarioId;
        this.cantidad = cantidad;
        this.estado = EstadoReserva.PENDIENTE;
        this.expiraEn = ahora.plus(ttlMinutos, ChronoUnit.MINUTES);
        this.creadaEn = ahora;
    }

    /** CP01/CP02: confirma la reserva PENDIENTE no vencida y emite las entradas. */
    public List<Entrada> confirmar(Clock reloj) {
        if (estado != EstadoReserva.PENDIENTE) {
            throw new TransicionInvalidaException("Solo se confirma una reserva PENDIENTE (actual: " + estado + ")");
        }
        if (vencida(reloj)) {
            throw new ReservaExpiradaException();
        }
        this.estado = EstadoReserva.CONFIRMADA;
        this.confirmadaEn = Instant.now(reloj);
        return Entrada.emitir(id, eventoId, cantidad);
    }

    /** RN5: cancela una reserva confirmada hasta `horasMinimas` antes del evento. */
    public void cancelar(Clock reloj, Instant fechaEvento, int horasMinimas) {
        if (estado != EstadoReserva.CONFIRMADA) {
            throw new TransicionInvalidaException("Solo se cancela una reserva CONFIRMADA (actual: " + estado + ")");
        }
        Instant limite = fechaEvento.minus(horasMinimas, ChronoUnit.HOURS);
        if (Instant.now(reloj).isAfter(limite)) {
            throw new CancelacionFueraDePlazoException();
        }
        this.estado = EstadoReserva.CANCELADA;
        this.canceladaEn = Instant.now(reloj);
    }

    /** RN3: pasa a EXPIRADA una reserva pendiente vencida (lo usa el job del worker). */
    public void expirar() {
        if (estado != EstadoReserva.PENDIENTE) {
            throw new TransicionInvalidaException("Solo expira una reserva PENDIENTE (actual: " + estado + ")");
        }
        this.estado = EstadoReserva.EXPIRADA;
    }

    public boolean vencida(Clock reloj) {
        return expiraEn != null && !expiraEn.isAfter(Instant.now(reloj));
    }

    public boolean esDe(UUID usuario) {
        return usuarioId.equals(usuario);
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventoId() {
        return eventoId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public int getCantidad() {
        return cantidad;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public Instant getExpiraEn() {
        return expiraEn;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Instant getConfirmadaEn() {
        return confirmadaEn;
    }

    public Instant getCanceladaEn() {
        return canceladaEn;
    }
}
