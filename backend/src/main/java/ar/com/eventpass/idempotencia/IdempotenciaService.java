package ar.com.eventpass.idempotencia;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotenciaService {

    private final IdempotenciaRepository repo;

    public IdempotenciaService(IdempotenciaRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public Optional<String> buscarRespuesta(String clave, UUID usuarioId) {
        return repo.findByClaveAndUsuarioId(clave, usuarioId).map(IdempotencyKey::getRespuesta);
    }

    @Transactional
    public void guardar(String clave, UUID usuarioId, String endpoint, int statusCode, String respuestaJson) {
        if (repo.findByClaveAndUsuarioId(clave, usuarioId).isEmpty()) {
            repo.save(new IdempotencyKey(clave, usuarioId, endpoint, statusCode, respuestaJson));
        }
    }
}
