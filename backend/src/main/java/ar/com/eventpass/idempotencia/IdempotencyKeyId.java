package ar.com.eventpass.idempotencia;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Clave primaria compuesta (clave, usuario) para IdempotencyKey. */
public class IdempotencyKeyId implements Serializable {

    private String clave;
    private UUID usuarioId;

    public IdempotencyKeyId() {
    }

    public IdempotencyKeyId(String clave, UUID usuarioId) {
        this.clave = clave;
        this.usuarioId = usuarioId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IdempotencyKeyId that)) {
            return false;
        }
        return Objects.equals(clave, that.clave) && Objects.equals(usuarioId, that.usuarioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clave, usuarioId);
    }
}
