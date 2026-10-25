package ar.com.eventpass.entrada;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntradaRepository extends JpaRepository<Entrada, UUID> {

    List<Entrada> findByReservaId(UUID reservaId);

    List<Entrada> findByReservaIdIn(List<UUID> reservaIds);

    Optional<Entrada> findByCodigo(String codigo);
}
