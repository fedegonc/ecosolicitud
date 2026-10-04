package com.ecosolicitud.guia.interno;

import com.ecosolicitud.guia.ArticuloSemilla;
import com.ecosolicitud.shared.Material;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

// Contenido editorial de referencia. Sin fecha: una guía no envejece.
// El material es opcional: hay artículos generales y artículos de un residuo.
@Entity
@Table(name = "articulos")
@Getter
public class Articulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 4000)
    private String cuerpo;

    @Enumerated(EnumType.STRING)
    private Material material;

    @Column(nullable = false)
    private int orden;

    protected Articulo() {
    }

    public static Articulo nuevo(ArticuloSemilla s) {
        var a = new Articulo();
        a.titulo = s.titulo();
        a.cuerpo = s.cuerpo();
        a.material = s.material();
        a.orden = s.orden();
        return a;
    }
}
