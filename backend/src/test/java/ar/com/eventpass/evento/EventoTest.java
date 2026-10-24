package ar.com.eventpass.evento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import ar.com.eventpass.comun.CupoTotalInvalidoException;
import ar.com.eventpass.comun.TransicionInvalidaException;

/** Unit tests del dominio Evento: transiciones de estado (patrón State) y RN7. */
class EventoTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private static final Instant FUTURO = Instant.parse("2026-06-01T00:00:00Z");
    private static final Instant PASADO = Instant.parse("2025-01-01T00:00:00Z");

    private Evento nuevo(int cupo, Instant fecha) {
        return new Evento(UUID.randomUUID(), "Charla", "desc", "Aula 1", fecha, cupo, null);
    }

    @Test
    @DisplayName("Publicar un BORRADOR futuro lo pasa a PUBLICADO")
    void publicarOk() {
        Evento e = nuevo(50, FUTURO);
        e.publicar(RELOJ);
        assertThat(e.getEstado()).isEqualTo(EstadoEvento.PUBLICADO);
    }

    @Test
    @DisplayName("Publicar dos veces lanza TransicionInvalida")
    void publicarDosVeces() {
        Evento e = nuevo(50, FUTURO);
        e.publicar(RELOJ);
        assertThatThrownBy(() -> e.publicar(RELOJ)).isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    @DisplayName("No se puede publicar un evento con fecha pasada")
    void publicarPasado() {
        Evento e = nuevo(50, PASADO);
        assertThatThrownBy(() -> e.publicar(RELOJ)).isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    @DisplayName("Publicar un evento cancelado lanza TransicionInvalida")
    void publicarCancelado() {
        Evento e = nuevo(50, FUTURO);
        e.cancelar();
        assertThatThrownBy(() -> e.publicar(RELOJ)).isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    @DisplayName("RN7: no se puede bajar el cupo total por debajo de lo comprometido")
    void rn7Baja() {
        Evento e = nuevo(10, FUTURO);
        ReflectionTestUtils.setField(e, "cupoDisponible", 3); // 7 comprometidas
        assertThatThrownBy(() -> e.cambiarCupoTotal(5)).isInstanceOf(CupoTotalInvalidoException.class);
    }

    @Test
    @DisplayName("RN7: subir el cupo recalcula el disponible preservando lo comprometido")
    void rn7Sube() {
        Evento e = nuevo(10, FUTURO);
        ReflectionTestUtils.setField(e, "cupoDisponible", 3); // 7 comprometidas
        e.cambiarCupoTotal(12);
        assertThat(e.getCupoTotal()).isEqualTo(12);
        assertThat(e.getCupoDisponible()).isEqualTo(5); // 12 - 7
    }

    @Test
    @DisplayName("Sin entradas comprometidas, cambiar el cupo ajusta el disponible completo")
    void cambiarCupoSinComprometidas() {
        Evento e = nuevo(10, FUTURO);
        e.cambiarCupoTotal(20);
        assertThat(e.getCupoDisponible()).isEqualTo(20);
    }
}
