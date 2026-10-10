package com.ecosolicitud.opinion.interno;

import java.time.Instant;

import com.ecosolicitud.shared.Rol;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

// Respuesta al cuestionario de respaldo; queda guardada aunque el correo falle.
@Entity
@Table(name = "respuestas_respaldo")
@Getter
public class RespuestaRespaldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false, length = 2)
    private String idioma;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(nullable = false)
    private Instant enviadaEn;

    @Column(nullable = false)
    private boolean enviadaPorCorreo;

    protected RespuestaRespaldo() {
    }

    RespuestaRespaldo(Rol rol, String idioma, String contenido, Instant enviadaEn) {
        this.rol = rol;
        this.idioma = idioma;
        this.contenido = contenido;
        this.enviadaEn = enviadaEn;
    }

    void marcarEnviadaPorCorreo() {
        this.enviadaPorCorreo = true;
    }
}
