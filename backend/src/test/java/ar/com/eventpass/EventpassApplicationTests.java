package ar.com.eventpass;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test de la Fase 0: el contexto arranca contra un PostgreSQL 16 real
 * (Testcontainers, nada de H2) y Flyway corre sin romper.
 * Requiere Docker Desktop corriendo.
 */
@SpringBootTest
@Testcontainers
class EventpassApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    @DisplayName("El contexto de Spring arranca con Postgres y Flyway")
    void contextLoads() {
    }
}
