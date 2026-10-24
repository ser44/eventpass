package ar.com.eventpass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend EventPass.
 * Un solo código, dos perfiles de ejecución: `api` (HTTP) y `worker` (jobs).
 */
@SpringBootApplication
public class EventpassApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventpassApplication.class, args);
    }
}
