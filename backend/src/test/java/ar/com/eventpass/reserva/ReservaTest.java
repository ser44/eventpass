package ar.com.eventpass.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.com.eventpass.comun.CancelacionFueraDePlazoException;
import ar.com.eventpass.comun.ReservaExpiradaException;
import ar.com.eventpass.comun.TransicionInvalidaException;
import ar.com.eventpass.entrada.Entrada;
import ar.com.eventpass.entrada.EstadoEntrada;

/** Unit tests del dominio Reserva: CP01, CP02 y transiciones (RN2, RN5, expirar). */
class ReservaTest {

    private static final Instant T0 = Instant.parse("2026-01-01T12:00:00Z");

    private Clock en(Instant i) {
        return Clock.fixed(i, ZoneOffset.UTC);
    }

    private Reserva reservaDe(int cantidad) {
        return new Reserva(UUID.randomUUID(), UUID.randomUUID(), cantidad, en(T0), 10);
    }

    @Test
    @DisplayName("CP01: confirmar una reserva PENDIENTE de 3, no vencida, emite 3 entradas VALIDA únicas")
    void cp01() {
        Reserva r = reservaDe(3);
        List<Entrada> entradas = r.confirmar(en(T0.plus(Duration.ofMinutes(5))));

        assertThat(r.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(entradas).hasSize(3);
        assertThat(entradas).allMatch(e -> e.getEstado() == EstadoEntrada.VALIDA);
        assertThat(entradas).extracting(Entrada::getCodigo).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("CP02: confirmar una reserva vencida lanza ReservaExpiradaException y no emite entradas")
    void cp02() {
        Reserva r = reservaDe(2);
        assertThatThrownBy(() -> r.confirmar(en(T0.plus(Duration.ofMinutes(11)))))
            .isInstanceOf(ReservaExpiradaException.class);
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
    }

    @Test
    @DisplayName("RN2: una reserva con cantidad fuera de 1..4 se rechaza")
    void rn2() {
        assertThatThrownBy(() -> reservaDe(5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reservaDe(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("No se puede confirmar dos veces (transición inválida)")
    void confirmarDosVeces() {
        Reserva r = reservaDe(1);
        r.confirmar(en(T0.plus(Duration.ofMinutes(1))));
        assertThatThrownBy(() -> r.confirmar(en(T0.plus(Duration.ofMinutes(2)))))
            .isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    @DisplayName("RN5: cancelar una reserva confirmada con más de 48 h de margen la deja CANCELADA")
    void rn5Cancelable() {
        Reserva r = reservaDe(2);
        r.confirmar(en(T0.plus(Duration.ofMinutes(1))));
        Instant fechaEvento = T0.plus(Duration.ofDays(10));
        r.cancelar(en(T0.plus(Duration.ofMinutes(2))), fechaEvento, 48);
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    @DisplayName("RN5: cancelar a menos de 48 h del evento lanza CancelacionFueraDePlazo")
    void rn5FueraDePlazo() {
        Reserva r = reservaDe(2);
        r.confirmar(en(T0.plus(Duration.ofMinutes(1))));
        Instant fechaEvento = T0.plus(Duration.ofHours(1));
        assertThatThrownBy(() -> r.cancelar(en(T0.plus(Duration.ofMinutes(2))), fechaEvento, 48))
            .isInstanceOf(CancelacionFueraDePlazoException.class);
    }

    @Test
    @DisplayName("Expirar solo aplica a una reserva PENDIENTE")
    void expirar() {
        Reserva r = reservaDe(1);
        r.expirar();
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.EXPIRADA);
        assertThatThrownBy(r::expirar).isInstanceOf(TransicionInvalidaException.class);
    }
}
