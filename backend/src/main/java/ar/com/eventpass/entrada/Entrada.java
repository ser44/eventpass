package ar.com.eventpass.entrada;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ar.com.eventpass.comun.EntradaYaUsadaException;
import ar.com.eventpass.comun.TransicionInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entrada emitida al confirmar una reserva. Dominio rico (patrón State):
 * se valida una sola vez (RN6) y puede anularse al cancelar la reserva.
 */
@Entity
@Table(name = "entrada")
public class Entrada {

    @Id
    private UUID id;

    @Column(name = "reserva_id", nullable = false)
    private UUID reservaId;

    @Column(name = "evento_id", nullable = false)
    private UUID eventoId;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEntrada estado;

    @Column(name = "usada_en")
    private Instant usadaEn;

    @Column(name = "validada_por")
    private UUID validadaPor;

    protected Entrada() {
    }

    private Entrada(UUID reservaId, UUID eventoId, String codigo) {
        this.id = UUID.randomUUID();
        this.reservaId = reservaId;
        this.eventoId = eventoId;
        this.codigo = codigo;
        this.estado = EstadoEntrada.VALIDA;
    }

    /** Emite `cantidad` entradas VALIDA con códigos únicos para una reserva. */
    public static List<Entrada> emitir(UUID reservaId, UUID eventoId, int cantidad) {
        List<Entrada> entradas = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) {
            entradas.add(new Entrada(reservaId, eventoId, GeneradorCodigo.generar()));
        }
        return entradas;
    }

    /** RN6: valida el ingreso una sola vez. El segundo escaneo se rechaza. */
    public void validarIngreso(Clock reloj, UUID usuarioStaff) {
        if (estado == EstadoEntrada.USADA) {
            throw new EntradaYaUsadaException(usadaEn);
        }
        if (estado == EstadoEntrada.ANULADA) {
            throw new TransicionInvalidaException("La entrada está anulada");
        }
        this.estado = EstadoEntrada.USADA;
        this.usadaEn = Instant.now(reloj);
        this.validadaPor = usuarioStaff;
    }

    public void anular() {
        if (estado == EstadoEntrada.USADA) {
            throw new TransicionInvalidaException("No se puede anular una entrada ya utilizada");
        }
        this.estado = EstadoEntrada.ANULADA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getReservaId() {
        return reservaId;
    }

    public UUID getEventoId() {
        return eventoId;
    }

    public String getCodigo() {
        return codigo;
    }

    public EstadoEntrada getEstado() {
        return estado;
    }

    public Instant getUsadaEn() {
        return usadaEn;
    }

    public UUID getValidadaPor() {
        return validadaPor;
    }
}
