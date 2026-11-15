package com.ecosolicitud.solicitud;

public record TableroSolicitudes(OrganizacionTablero organizacion,
        BandejaSolicitudes bandeja, SolicitudMetricas semana) {

    public record OrganizacionTablero(String nombre) {
    }
}
