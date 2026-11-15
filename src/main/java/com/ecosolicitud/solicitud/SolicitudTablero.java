package com.ecosolicitud.solicitud;

import java.time.Instant;

record SolicitudTablero(SolicitudInfo info, Instant respondidaEn,
        Instant aceptadaEn) implements SolicitudMedicion {

    @Override
    public Estado getEstado() {
        return info.estado();
    }

    @Override
    public Instant getCreadaEn() {
        return info.creadaEn();
    }

    @Override
    public Instant getRespondidaEn() {
        return respondidaEn;
    }

    @Override
    public Instant getAceptadaEn() {
        return aceptadaEn;
    }
}
