package ar.com.eventpass.notificacion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MensajeOutboxRepository extends JpaRepository<MensajeOutbox, UUID> {

    /**
     * Toma un lote de mensajes pendientes listos para enviar, bloqueándolos con
     * FOR UPDATE SKIP LOCKED para que varios workers no procesen el mismo.
     */
    @Query(value = """
        select * from mensaje_outbox
        where estado = 'PENDIENTE' and proximo_intento <= :ahora
        order by proximo_intento
        for update skip locked
        limit :limite
        """, nativeQuery = true)
    List<MensajeOutbox> tomarLote(@Param("ahora") Instant ahora, @Param("limite") int limite);

    long countByEstado(EstadoOutbox estado);
}
