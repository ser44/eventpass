package ar.com.eventpass.evento;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Descuento de cupo en UNA sola sentencia atómica (decisión central, §3.1).
     * Nunca leer-comparar-guardar en Java. Devuelve 0 si no hay cupo o el evento
     * no es reservable. El UPDATE JPQL no incrementa `version` (correcto).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Evento e set e.cupoDisponible = e.cupoDisponible - :cantidad
        where e.id = :id
          and e.estado = ar.com.eventpass.evento.EstadoEvento.PUBLICADO
          and e.fechaInicio > :ahora
          and e.cupoDisponible >= :cantidad
        """)
    int descontarCupo(@Param("id") UUID id, @Param("cantidad") int cantidad, @Param("ahora") Instant ahora);

    /** Restitución de cupo (expirar/cancelar), también atómica, sin pasarse del total. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Evento e set e.cupoDisponible = e.cupoDisponible + :cantidad
        where e.id = :id and e.cupoDisponible + :cantidad <= e.cupoTotal
        """)
    int restituirCupo(@Param("id") UUID id, @Param("cantidad") int cantidad);
}
