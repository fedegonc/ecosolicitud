package com.ecosolicitud.solicitud.interno;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.ecosolicitud.shared.Ciudad;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Getter;

@Entity
@Table(name = "solicitudes")
@Getter
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String ciudadanoId;
    private String nombreCiudadano;
    @Enumerated(EnumType.STRING)
    private Ciudad ciudad;
    private String direccion;
    private String referencia;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "solicitud_materiales", joinColumns = @JoinColumn(name = "solicitud_id"))
    @OrderColumn(name = "orden")
    @Enumerated(EnumType.STRING)
    @Column(name = "material")
    private List<Material> materiales = new ArrayList<>();
    private String organizacionId;
    private String nota;
    @Enumerated(EnumType.STRING)
    private Estado estado;
    private Instant creadaEn;
    private Instant finalizadaEn;
    @Version
    private long version;

    protected Solicitud() {
    }

    public static Solicitud nueva(SolicitudSemilla s) {
        if (s.estado().esFinal() != (s.finalizadaEn() != null)) {
            throw new IllegalArgumentException("finalizadaEn inconsistente con el estado");
        }
        if (s.finalizadaEn() != null && s.finalizadaEn().isBefore(s.creadaEn())) {
            throw new IllegalArgumentException("finalizadaEn anterior a creadaEn");
        }
        var sol = new Solicitud();
        sol.ciudadanoId = s.ciudadanoId();
        sol.nombreCiudadano = s.nombreCiudadano();
        sol.ciudad = s.ciudad();
        sol.direccion = s.direccion();
        sol.referencia = s.referencia();
        sol.materiales = new ArrayList<>(s.materiales());
        sol.organizacionId = s.organizacionId();
        sol.nota = s.nota();
        sol.estado = s.estado();
        sol.creadaEn = s.creadaEn();
        sol.finalizadaEn = s.finalizadaEn();
        return sol;
    }

    public void aceptar() {
        exigir(estado.permiteAceptar(), "aceptar");
        estado = Estado.EN_CURSO;
    }

    public void rechazar(Instant ahora) {
        exigir(estado.permiteRechazar(), "rechazar");
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
