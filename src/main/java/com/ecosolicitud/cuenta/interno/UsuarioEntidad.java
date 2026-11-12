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
import org.hibernate.annotations.ColumnDefault;

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

    // opcional: solo quien lo carga puede recuperar la clave por correo
    @Column(unique = true, length = Limites.EMAIL)
    private String email;

    // default sí: las cuentas existentes quedan activas; una baja no entra
    // más pero conserva su historial de solicitudes
    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean activo = true;

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

    public void renovarClaveHash(String claveHash) {
        this.claveHash = claveHash;
    }

    public void cambiarDatos(String nombre, String telefono, String email) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
    }

    public void cambiarEmail(String email) {
        this.email = email;
    }

    public void cambiarActivo(boolean activo) {
        this.activo = activo;
    }
}
