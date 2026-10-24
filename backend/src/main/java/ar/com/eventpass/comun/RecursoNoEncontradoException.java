package ar.com.eventpass.comun;

import org.springframework.http.HttpStatus;

public class RecursoNoEncontradoException extends DominioException {

    public RecursoNoEncontradoException(String recurso) {
        super("No se encontró el recurso: " + recurso);
    }

    @Override
    public String getCode() {
        return "NO_ENCONTRADO";
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
