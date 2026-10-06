package com.ecosolicitud.shared;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

// Catálogo único de materiales. El código es la clave de negocio estable:
// formularios, reglas e igualdad lo usan; el id es solo la FK física.
@Entity
@Table(name = "materiales")
@Getter
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String icono;

    @Column(nullable = false)
    private boolean activo;

    protected Material() {
    }

    public Material(String codigo, String nombre, String icono) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.icono = icono;
        this.activo = true;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Material m && Objects.equals(codigo, m.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(codigo);
    }

    @Override
    public String toString() {
        return codigo;
    }
}
