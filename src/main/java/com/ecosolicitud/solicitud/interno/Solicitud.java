package com.ecosolicitud.solicitud.interno;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.ecosolicitud.shared.Material;
import com.ecosolicitud.solicitud.Estado;
import com.ecosolicitud.solicitud.SolicitudSemilla;
import com.ecosolicitud.solicitud.TransicionInvalidaException;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "solicitudes", indexes = {
        @Index(name = "ix_sol_ciudadano", columnList = "ciudadano_id, creada_en"),
        @Index(name = "ix_sol_org", columnList = "organizacion_id, estado")})
@Getter
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ciudadano_id")
    private Ciudadano ciudadano;
    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String direccion;
    @Size(max = 120)
    @Column(length = 120)
    private String referencia;
    @NotEmpty
    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "solicitud_materiales", joinColumns = @JoinColumn(name = "solicitud_id"))
    @OrderColumn(name = "orden")
    @Enumerated(EnumType.STRING)
    @Column(name = "material")
    private List<Material> materiales = new ArrayList<>();
    private String organizacionId;
    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String contacto;
    @Size(max = 300)
    @Column(length = 300)
    private String nota;
    @Enumerated(EnumType.STRING)
    private Estado estado;
    private Instant creadaEn;
    private Instant respondidaEn;
    private Instant finalizadaEn;
    @Version
    private long version;

    protected Solicitud() {
    }

    public static Solicitud nueva(SolicitudSemilla s, Ciudadano ciudadano) {
        if (s.estado().esFinal() != (s.finalizadaEn() != null)) {
            throw new IllegalArgumentException("finalizadaEn inconsistente con el estado");
        }
        if (s.finalizadaEn() != null && s.finalizadaEn().isBefore(s.creadaEn())) {
            throw new IllegalArgumentException("finalizadaEn anterior a creadaEn");
        }
        if (s.respondidaEn() != null && s.estado() == Estado.PENDIENTE) {
            throw new IllegalArgumentException("respondidaEn en una pendiente");
        }
        if (s.respondidaEn() != null && s.respondidaEn().isBefore(s.creadaEn())) {
            throw new IllegalArgumentException("respondidaEn anterior a creadaEn");
        }
        var sol = new Solicitud();
        sol.ciudadano = ciudadano;
        sol.direccion = s.direccion();
        sol.referencia = s.referencia();
        sol.materiales = new ArrayList<>(s.materiales());
        sol.organizacionId = s.organizacionId();
        sol.contacto = s.contacto();
        sol.nota = s.nota();
        sol.estado = s.estado();
        sol.creadaEn = s.creadaEn();
        sol.respondidaEn = s.respondidaEn();
        sol.finalizadaEn = s.finalizadaEn();
        return sol;
    }

    public void aceptar(Instant ahora) {
        exigir(estado.permiteAceptar(), "aceptar");
        estado = Estado.EN_CURSO;
        respondidaEn = ahora;
    }

    public void rechazar(Instant ahora) {
        exigir(estado.permiteRechazar(), "rechazar");
        if (respondidaEn == null) {
            respondidaEn = ahora;
        }
        cerrar(Estado.RECHAZADA, ahora);
    }

    public void completar(Instant ahora) {
        exigir(estado.permiteCompletar(), "completar");
        cerrar(Estado.COMPLETADA, ahora);
    }

    public void cancelar(Instant ahora) {
        exigir(estado.permiteCancelar(), "cancelar");
        cerrar(Estado.CANCELADA, ahora);
    }

    private void exigir(boolean permitido, String accion) {
        if (!permitido) {
            throw new TransicionInvalidaException(estado, accion);
        }
    }

    private void cerrar(Estado destino, Instant ahora) {
        estado = destino;
        finalizadaEn = ahora;
    }
}
