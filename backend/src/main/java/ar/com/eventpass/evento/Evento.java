package ar.com.eventpass.evento;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import ar.com.eventpass.comun.CupoTotalInvalidoException;
import ar.com.eventpass.comun.TransicionInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Evento con cupo limitado. Dominio rico: el cupo y el estado solo cambian por
 * métodos con sentido de negocio (patrón State). El descuento de cupo por
 * reservas NO pasa por acá: se hace con un UPDATE atómico (ver Fase 3).
 */
@Entity
@Table(name = "evento")
public class Evento {

    @Id
    private UUID id;

    @Column(name = "organizador_id", nullable = false)
    private UUID organizadorId;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(nullable = false)
    private String lugar;

    @Column(name = "fecha_inicio", nullable = false)
    private Instant fechaInicio;

    @Column(name = "cupo_total", nullable = false)
    private int cupoTotal;

    @Column(name = "cupo_disponible", nullable = false)
    private int cupoDisponible;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEvento estado;

    @Column(name = "imagen_url")
    private String imagenUrl;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected Evento() {
    }

    public Evento(UUID organizadorId, String titulo, String descripcion, String lugar,
                  Instant fechaInicio, int cupoTotal, String imagenUrl) {
        this.id = UUID.randomUUID();
        this.organizadorId = organizadorId;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.lugar = lugar;
        this.fechaInicio = fechaInicio;
        this.cupoTotal = cupoTotal;
        this.cupoDisponible = cupoTotal;
        this.estado = EstadoEvento.BORRADOR;
        this.imagenUrl = imagenUrl;
        this.creadoEn = Instant.now();
        this.actualizadoEn = this.creadoEn;
    }

    public void publicar(Clock reloj) {
        if (estado != EstadoEvento.BORRADOR) {
            throw new TransicionInvalidaException("Solo se publica un evento en BORRADOR (estado actual: " + estado + ")");
        }
        if (!fechaInicio.isAfter(Instant.now(reloj))) {
            throw new TransicionInvalidaException("No se puede publicar un evento con fecha pasada");
        }
        estado = EstadoEvento.PUBLICADO;
        tocar();
    }

    public void cancelar() {
        if (estado == EstadoEvento.CANCELADO || estado == EstadoEvento.FINALIZADO) {
            throw new TransicionInvalidaException("No se puede cancelar un evento en estado " + estado);
        }
        estado = EstadoEvento.CANCELADO;
        tocar();
    }

    public void actualizarDatos(String titulo, String descripcion, String lugar,
                                Instant fechaInicio, String imagenUrl) {
        if (estado == EstadoEvento.CANCELADO || estado == EstadoEvento.FINALIZADO) {
            throw new TransicionInvalidaException("No se puede editar un evento en estado " + estado);
        }
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.lugar = lugar;
        this.fechaInicio = fechaInicio;
        this.imagenUrl = imagenUrl;
        tocar();
    }

    /** RN7: no bajar el cupo total por debajo de lo ya comprometido (retenido/emitido). */
    public void cambiarCupoTotal(int nuevoCupoTotal) {
        int comprometidas = cupoTotal - cupoDisponible;
        if (nuevoCupoTotal < comprometidas) {
            throw new CupoTotalInvalidoException(nuevoCupoTotal, comprometidas);
        }
        this.cupoDisponible = nuevoCupoTotal - comprometidas;
        this.cupoTotal = nuevoCupoTotal;
        tocar();
    }

    public boolean esDe(UUID usuarioId) {
        return organizadorId.equals(usuarioId);
    }

    private void tocar() {
        this.actualizadoEn = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizadorId() {
        return organizadorId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getLugar() {
        return lugar;
    }

    public Instant getFechaInicio() {
        return fechaInicio;
    }

    public int getCupoTotal() {
        return cupoTotal;
    }

    public int getCupoDisponible() {
        return cupoDisponible;
    }

    public EstadoEvento getEstado() {
        return estado;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }
}
