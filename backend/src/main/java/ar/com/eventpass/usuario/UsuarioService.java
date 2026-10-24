package ar.com.eventpass.usuario;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.eventpass.comun.CredencialesInvalidasException;
import ar.com.eventpass.comun.EmailDuplicadoException;
import ar.com.eventpass.comun.RecursoNoEncontradoException;
import ar.com.eventpass.usuario.dto.ActualizarUsuarioRequest;
import ar.com.eventpass.usuario.dto.LoginRequest;
import ar.com.eventpass.usuario.dto.LoginResponse;
import ar.com.eventpass.usuario.dto.RegistroRequest;
import ar.com.eventpass.usuario.dto.UsuarioDto;

@Service
public class UsuarioService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder, JwtService jwt) {
        this.repo = repo;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /** Registro público: siempre crea un ASISTENTE. */
    @Transactional
    public UsuarioDto registrar(RegistroRequest req) {
        if (repo.existsByEmail(req.email())) {
            throw new EmailDuplicadoException(req.email());
        }
        Usuario u = new Usuario(req.email(), req.nombre(), encoder.encode(req.password()), Rol.ASISTENTE);
        return UsuarioDto.desde(repo.save(u));
    }

    @Transactional(readOnly = true)
    public LoginResponse autenticar(LoginRequest req) {
        Usuario u = repo.findByEmail(req.email())
            .filter(Usuario::isActivo)
            .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
            .orElseThrow(CredencialesInvalidasException::new);
        return new LoginResponse(jwt.generar(u), UsuarioDto.desde(u));
    }

    @Transactional(readOnly = true)
    public UsuarioDto buscarPorId(UUID id) {
        return repo.findById(id).map(UsuarioDto::desde)
            .orElseThrow(() -> new RecursoNoEncontradoException("usuario " + id));
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto> listar() {
        return repo.findAll().stream().map(UsuarioDto::desde).toList();
    }

    @Transactional
    public UsuarioDto actualizar(UUID id, ActualizarUsuarioRequest req) {
        Usuario u = repo.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("usuario " + id));
        if (req.rol() != null) {
            u.cambiarRol(req.rol());
        }
        if (req.activo() != null) {
            if (req.activo()) {
                u.activar();
            } else {
                u.desactivar();
            }
        }
        return UsuarioDto.desde(repo.save(u));
    }
}
