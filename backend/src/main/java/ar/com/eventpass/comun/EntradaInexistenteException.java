package ar.com.eventpass.comun;

import org.springframework.http.HttpStatus;

/** No existe ninguna entrada con ese código. */
public class EntradaInexistenteException extends DominioException {

    public EntradaInexistenteException() {
        super("No existe una entrada con ese código");
    }

    @Override
    public String getCode() {
        return "ENTRADA_INEXISTENTE";
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
