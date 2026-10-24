package ar.com.eventpass.comun;

import org.springframework.http.HttpStatus;

public class CredencialesInvalidasException extends DominioException {

    public CredencialesInvalidasException() {
        super("Email o contraseña incorrectos");
    }

    @Override
    public String getCode() {
        return "CREDENCIALES_INVALIDAS";
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
