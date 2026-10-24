package ar.com.eventpass.evento;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoRepository extends JpaRepository<Evento, UUID> {

    /**
     * Catálogo público: solo PUBLICADO y con fecha futura, con filtros opcionales.
     * Los filtros se pasan ya normalizados (q = "" para "sin filtro", y rangos de
     * fecha con centinelas) para evitar parámetros null que Postgres no puede tipar.
     */
    @Query("""
        select e from Evento e
        where e.estado = ar.com.eventpass.evento.EstadoEvento.PUBLICADO
          and e.fechaInicio > :ahora
          and lower(e.titulo) like lower(concat('%', :q, '%'))
          and e.fechaInicio >= :desde
          and e.fechaInicio <= :hasta
        """)
    Page<Evento> buscarPublicados(
        @Param("ahora") Instant ahora,
        @Param("q") String q,
        @Param("desde") Instant desde,
        @Param("hasta") Instant hasta,
        Pageable pageable);

    List<Evento> findByOrganizadorIdOrderByCreadoEnDesc(UUID organizadorId);
}
