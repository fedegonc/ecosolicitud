package com.ecosolicitud.solicitud.interno;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

@Entity
@Table(name = "ciudadanos")
@Getter
public class Ciudadano {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 80)
    private String nombre;

    protected Ciudadano() {
    }

    public Ciudadano(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public void renombrar(String nombre) {
        this.nombre = nombre;
    }
}
