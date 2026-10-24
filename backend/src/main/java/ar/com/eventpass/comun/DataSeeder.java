package ar.com.eventpass.comun;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ar.com.eventpass.usuario.Rol;
import ar.com.eventpass.usuario.Usuario;
import ar.com.eventpass.usuario.UsuarioRepository;

/**
 * Carga usuarios semilla en los perfiles dev/demo. Idempotente: solo crea los
 * que faltan, así se puede reiniciar sin duplicar.
 */
@Component
@Profile({"dev", "demo"})
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    public DataSeeder(UsuarioRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        crear("admin@eventpass.dev", "Admin", "Admin123!", Rol.ADMIN);
        crear("orga@eventpass.dev", "Organizador Demo", "Orga123!", Rol.ORGANIZADOR);
        for (int i = 1; i <= 5; i++) {
            crear("asistente" + i + "@eventpass.dev", "Asistente " + i, "Asis123!", Rol.ASISTENTE);
        }
    }

    private void crear(String email, String nombre, String password, Rol rol) {
        if (!repo.existsByEmail(email)) {
            repo.save(new Usuario(email, nombre, encoder.encode(password), rol));
        }
    }
}
