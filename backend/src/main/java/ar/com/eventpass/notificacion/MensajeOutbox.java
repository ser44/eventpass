package ar.com.eventpass.notificacion;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Mensaje pendiente de enviar (Transactional Outbox). Se inserta en la misma tx que la reserva. */
@Entity
@Table(name = "mensaje_outbox")
public class MensajeOutbox {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String tipo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOutbox estado;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "proximo_intento", nullable = false)
    private Instant proximoIntento;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "enviado_en")
    private Instant enviadoEn;

    @Column(name = "ultimo_error", columnDefinition = "text")
    private String ultimoError;

    protected MensajeOutbox() {
    }

    public static MensajeOutbox crear(String tipo, String payload) {
        MensajeOutbox m = new MensajeOutbox();
        m.id = UUID.randomUUID();
        m.tipo = tipo;
        m.payload = payload;
        m.estado = EstadoOutbox.PENDIENTE;
        m.intentos = 0;
        m.creadoEn = Instant.now();
        m.proximoIntento = m.creadoEn;
        return m;
    }

    public void marcarEnviado(Clock reloj) {
        this.estado = EstadoOutbox.ENVIADO;
        this.enviadoEn = Instant.now(reloj);
    }

    /** Suma un intento; tras `maxIntentos` queda FALLIDO, si no reprograma con backoff lineal. */
    public void registrarFallo(Clock reloj, String error, int maxIntentos, int backoffSegundosBase) {
        this.intentos++;
        this.ultimoError = error;
        if (this.intentos >= maxIntentos) {
            this.estado = EstadoOutbox.FALLIDO;
        } else {
            this.proximoIntento = Instant.now(reloj).plus((long) backoffSegundosBase * intentos, ChronoUnit.SECONDS);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getPayload() {
        return payload;
    }

    public EstadoOutbox getEstado() {
        return estado;
    }

    public int getIntentos() {
        return intentos;
    }
}
