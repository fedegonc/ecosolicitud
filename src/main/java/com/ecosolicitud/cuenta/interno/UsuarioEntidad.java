package com.ecosolicitud.cuenta.interno;

import com.ecosolicitud.shared.Limites;
import com.ecosolicitud.shared.Rol;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "usuarios")
@Getter
public class UsuarioEntidad {

    @Id
    @Column(length = 64)
    private String id;
    @Column(nullable = false, unique = true, length = 20)
    private String telefono;
    @Column(nullable = false, length = 60)
    private String claveHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;
    private String organizacionId;
    @Column(nullable = false, unique = true, length = Limites.NOMBRE)
    private String nombre;

    protected UsuarioEntidad() {
    }

    public UsuarioEntidad(String id, String telefono, String claveHash, Rol rol,
            String organizacionId, String nombre) {
        this.id = id;
        this.telefono = telefono;
        this.claveHash = claveHash;
        this.rol = rol;
        this.organizacionId = organizacionId;
        this.nombre = nombre;
    }
}
