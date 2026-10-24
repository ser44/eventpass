package ar.com.eventpass.comun;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce excepciones a respuestas Problem Details (RFC 7807) con `code` estable. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DominioException.class)
    public ProblemDetail manejarDominio(DominioException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        pd.setProperty("code", ex.getCode());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail manejarValidacion(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos inválidos");
        pd.setProperty("code", "VALIDACION");
        List<Map<String, String>> errores = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> Map.of(
                "campo", fe.getField(),
                "mensaje", fe.getDefaultMessage() == null ? "inválido" : fe.getDefaultMessage()))
            .toList();
        pd.setProperty("errores", errores);
        return pd;
    }
}
