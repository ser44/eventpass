package ar.com.eventpass.comun;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa los jobs programados SOLO en el perfil worker. Así la api (sin este
 * perfil) tiene los beans de los jobs pero su @Scheduled nunca dispara.
 */
@Configuration
@Profile("worker")
@EnableScheduling
public class WorkerConfig {
}
