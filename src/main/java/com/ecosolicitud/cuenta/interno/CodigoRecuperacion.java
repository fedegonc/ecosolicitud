package com.ecosolicitud.cuenta.interno;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

// Un código de 6 dígitos por usuario a la vez: el último pedido anula los
// anteriores. El código viaja en claro por correo; en la base queda el hash.
@Entity
@Table(name = "codigos_recuperacion")
@Getter
public class CodigoRecuperacion {

    static final int INTENTOS_MAXIMOS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String usuarioId;

    @Column(nullable = false, length = 60)
    private String codigoHash;

    @Column(nullable = false)
    private Instant creadaEn;

    @Column(nullable = false)
    private Instant expiraEn;

    @Column(nullable = false)
    private int intentos;

    @Column(nullable = false)
    private boolean usada;

    protected CodigoRecuperacion() {
    }

    public CodigoRecuperacion(String usuarioId, String codigoHash, Instant expiraEn) {
        this.usuarioId = usuarioId;
        this.codigoHash = codigoHash;
        this.creadaEn = Instant.now();
        this.expiraEn = expiraEn;
    }

    public boolean vigente(Instant ahora) {
        return !usada && intentos < INTENTOS_MAXIMOS && expiraEn.isAfter(ahora);
    }

    public void anotarIntento() {
        intentos++;
    }

    public void marcarUsada() {
        usada = true;
    }
}
