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

// Una valoración de facilidad de uso (1 a 5) con comentario opcional.
@Entity
@Table(name = "opiniones")
@Getter
public class Opinion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sección evaluada (name() de Seccion); null = valoración general.
    @Column(length = 20)
    private String seccion;

    @Column(nullable = false)
    private int valor;

    @Column(length = 500)
    private String comentario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private Instant creadaEn;

    protected Opinion() {
    }

    public Opinion(String seccion, int valor, String comentario, Rol rol,
            Instant creadaEn) {
        if (valor < 1 || valor > 5) {
            throw new IllegalArgumentException("valor fuera de 1 a 5");
        }
        this.seccion = seccion;
        this.valor = valor;
        this.comentario = comentario;
        this.rol = rol;
        this.creadaEn = creadaEn;
    }
}
