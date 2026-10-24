package ar.com.eventpass.comun;

import org.springframework.http.HttpStatus;

/**
 * Base de las excepciones de la aplicación. Cada subclase declara su `code`
 * estable (para el frontend) y el estado HTTP que le corresponde; el
 * {@link GlobalExceptionHandler} las traduce a Problem Details (RFC 7807).
 */
public abstract class DominioException extends RuntimeException {

    protected DominioException(String mensaje) {
        super(mensaje);
    }

    /** Código estable en MAYÚSCULAS, p. ej. CUPO_AGOTADO. */
    public abstract String getCode();

    public abstract HttpStatus getStatus();
}
