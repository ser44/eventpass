package ar.com.eventpass.idempotencia;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Respuesta guardada de una operación idempotente (misma clave + usuario -> misma respuesta). */
@Entity
@Table(name = "idempotency_key")
@IdClass(IdempotencyKeyId.class)
public class IdempotencyKey {

    @Id
    private String clave;

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false)
    private String endpoint;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String respuesta;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    protected IdempotencyKey() {
    }

    public IdempotencyKey(String clave, UUID usuarioId, String endpoint, int statusCode, String respuesta) {
        this.clave = clave;
        this.usuarioId = usuarioId;
        this.endpoint = endpoint;
        this.statusCode = statusCode;
        this.respuesta = respuesta;
        this.creadaEn = Instant.now();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getRespuesta() {
        return respuesta;
    }
}
