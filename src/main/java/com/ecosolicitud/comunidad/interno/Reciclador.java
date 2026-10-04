package com.ecosolicitud.comunidad.interno;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

// Registro de una persona que recicla informalmente por su cuenta.
// No está vinculada a ningún centro: es independiente, y esa es la clave.
// Solo se documenta quién es: no es usuario, no pide ni atiende solicitudes.
@Entity
@Table(name = "recicladores")
@Getter
public class Reciclador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String nombre;
    @Column(length = 30)
    private String telefono;

    protected Reciclador() {
    }

    public Reciclador(String nombre, String telefono) {
        this.nombre = nombre;
        this.telefono = telefono;
    }
}
