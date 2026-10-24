package ar.com.eventpass.usuario;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.com.eventpass.usuario.dto.ActualizarUsuarioRequest;
import ar.com.eventpass.usuario.dto.UsuarioDto;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UsuarioService service;

    public AdminController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioDto> listar() {
        return service.listar();
    }

    @PatchMapping("/{id}")
    public UsuarioDto actualizar(@PathVariable UUID id, @RequestBody ActualizarUsuarioRequest req) {
        return service.actualizar(id, req);
    }
}
