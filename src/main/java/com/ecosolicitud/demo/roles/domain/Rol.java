package com.ecosolicitud.demo.roles.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad Rol - Representa un rol en el sistema con nivel de jerarquía
 */
@Entity
@Table(name = "roles")
public class Rol {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nombre;
    private String descripcion;
    private Integer nivel; // Nivel jerárquico: mayor número = mayor jerarquía
    
    public Rol() {
    }
    
    public Rol(String nombre, String descripcion, Integer nivel) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.nivel = nivel;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public Integer getNivel() {
        return nivel;
    }
    
    public void setNivel(Integer nivel) {
        this.nivel = nivel;
    }
    
    /**
     * Verifica si este rol tiene mayor o igual jerarquía que otro rol
     */
    public boolean tieneJerarquiaSobre(Rol otroRol) {
        return this.nivel >= otroRol.getNivel();
    }
}
