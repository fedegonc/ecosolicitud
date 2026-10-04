package com.ecosolicitud.organizacion.interno;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;

// Registro de una persona que recicla informalmente y trabaja con un centro.
// Solo se documenta quién es: no es usuario, no pide ni atiende solicitudes.
@Entity
@Table(name = "recuperadores", indexes = @Index(name = "idx_rec_org", columnList = "organizacion_id"))
@Getter
public class Recuperador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacion_id", nullable = false)
    private Organizacion organizacion;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 30)
    private String telefono;

    protected Recuperador() {
    }

    public Recuperador(Organizacion organizacion, String nombre, String telefono) {
        this.organizacion = organizacion;
        this.nombre = nombre;
        this.telefono = telefono;
    }
}
