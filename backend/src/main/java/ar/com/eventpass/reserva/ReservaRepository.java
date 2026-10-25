package ar.com.eventpass.reserva;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    List<Reserva> findByUsuarioIdOrderByCreadaEnDesc(UUID usuarioId);

    /**
     * Toma reservas PENDIENTE vencidas, bloqueándolas con FOR UPDATE SKIP LOCKED
     * para que el job de expiración no pise a otro worker.
     */
    @Query(value = """
        select * from reserva
        where estado = 'PENDIENTE' and expira_en < :ahora
        order by expira_en
        for update skip locked
        limit :limite
        """, nativeQuery = true)
    List<Reserva> tomarVencidas(@Param("ahora") Instant ahora, @Param("limite") int limite);

    long countByEstadoAndExpiraEnBefore(EstadoReserva estado, Instant ahora);
}
