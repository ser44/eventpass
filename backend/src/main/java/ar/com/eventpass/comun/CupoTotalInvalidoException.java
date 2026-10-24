package ar.com.eventpass.comun;

import org.springframework.http.HttpStatus;

/** RN7: no se puede bajar el cupo total por debajo de las entradas ya comprometidas. */
public class CupoTotalInvalidoException extends DominioException {

    public CupoTotalInvalidoException(int nuevoCupo, int comprometidas) {
        super("El cupo total (" + nuevoCupo + ") no puede ser menor a las entradas ya comprometidas ("
            + comprometidas + ")");
    }

    @Override
    public String getCode() {
        return "CUPO_TOTAL_INVALIDO";
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
