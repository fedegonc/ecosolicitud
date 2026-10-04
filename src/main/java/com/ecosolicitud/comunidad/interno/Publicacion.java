package com.ecosolicitud.comunidad.interno;

import java.time.Instant;

import com.ecosolicitud.comunidad.PublicacionSemilla;
import com.ecosolicitud.comunidad.TipoPublicacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

// Contenido con fecha: novedades del servicio e historias de la comunidad.
@Entity
@Table(name = "publicaciones")
@Getter
public class Publicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 300)
    private String resumen;

    @Column(nullable = false, length = 4000)
    private String cuerpo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPublicacion tipo;

    @Column(nullable = false)
    private Instant publicadaEn;

    protected Publicacion() {
    }

    public static Publicacion nueva(PublicacionSemilla s) {
        var p = new Publicacion();
        p.titulo = s.titulo();
        p.resumen = s.resumen();
        p.cuerpo = s.cuerpo();
        p.tipo = s.tipo();
        p.publicadaEn = s.publicadaEn();
        return p;
    }
}
