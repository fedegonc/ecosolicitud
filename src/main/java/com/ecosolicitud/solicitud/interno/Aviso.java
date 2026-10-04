package com.ecosolicitud.solicitud.interno;

import java.time.Instant;

import com.ecosolicitud.solicitud.TipoAviso;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.Getter;

// Un aviso in-app para el destinatario de un cambio de estado.
@Entity
@Table(name = "avisos", indexes = @Index(name = "idx_aviso_destino", columnList = "destino"))
@Getter
public class Aviso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String destino;

    private Long solicitudId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAviso tipo;

    @Column(length = 80)
    private String detalle;

    @Column(nullable = false)
    private boolean leida;

    @Column(nullable = false)
    private Instant creadaEn;

    protected Aviso() {
    }

    public Aviso(String destino, Long solicitudId, TipoAviso tipo, String detalle,
            Instant creadaEn) {
        this.destino = destino;
        this.solicitudId = solicitudId;
        this.tipo = tipo;
        this.detalle = detalle;
        this.creadaEn = creadaEn;
    }

    public void marcarLeida() {
        leida = true;
    }
}
