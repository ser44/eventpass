package ar.com.eventpass.comun;

import java.util.List;

import org.springframework.data.domain.Page;

/** Envoltorio de paginación con los nombres de campo del contrato de la API. */
public record PaginaDto<T>(
    List<T> contenido,
    int pagina,
    int tamanio,
    long totalElementos,
    int totalPaginas
) {
    public static <E, D> PaginaDto<D> desde(Page<E> page, List<D> contenido) {
        return new PaginaDto<>(
            contenido,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages());
    }
}
