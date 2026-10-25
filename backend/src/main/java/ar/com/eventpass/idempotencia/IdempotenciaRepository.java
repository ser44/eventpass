package ar.com.eventpass.idempotencia;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotenciaRepository extends JpaRepository<IdempotencyKey, IdempotencyKeyId> {

    Optional<IdempotencyKey> findByClaveAndUsuarioId(String clave, UUID usuarioId);
}
