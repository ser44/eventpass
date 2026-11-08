package ar.com.eventpass.entrada;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface EntradaRepository extends JpaRepository<Entrada, UUID> {

    List<Entrada> findByReservaId(UUID reservaId);

    List<Entrada> findByReservaIdIn(List<UUID> reservaIds);

    Optional<Entrada> findByCodigo(String codigo);

    /**
     * Carga la entrada con bloqueo pesimista (SELECT ... FOR UPDATE) para que dos
     * check-ins simultáneos del mismo código se serialicen: solo uno la valida (RN6).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Entrada e where e.codigo = :codigo")
    Optional<Entrada> findByCodigoBloqueando(@Param("codigo") String codigo);

    long countByEventoIdAndEstado(UUID eventoId, EstadoEntrada estado);

    long countByEventoIdAndEstadoIn(UUID eventoId, Collection<EstadoEntrada> estados);
}
