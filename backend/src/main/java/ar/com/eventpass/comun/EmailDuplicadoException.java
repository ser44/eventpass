package ar.com.eventpass.comun;

import org.springframework.http.HttpStatus;

public class EmailDuplicadoException extends DominioException {

    public EmailDuplicadoException(String email) {
        super("Ya existe un usuario con el email " + email);
    }

    @Override
    public String getCode() {
        return "EMAIL_DUPLICADO";
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
