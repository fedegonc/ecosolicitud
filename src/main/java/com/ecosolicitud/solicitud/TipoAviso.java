package com.ecosolicitud.solicitud;

// Qué pasó con una solicitud, para contarlo en la bandeja de avisos.
// NUEVA y CANCELADA van para la organización; el resto para el ciudadano.
public enum TipoAviso {
    NUEVA("aviso.nueva"),
    ACEPTADA("aviso.aceptada"),
    RECHAZADA("aviso.rechazada"),
    COMPLETADA("aviso.completada"),
    CANCELADA("aviso.cancelada");

    private final String clave;

    TipoAviso(String clave) {
        this.clave = clave;
    }

    public String getClave() {
        return clave;
    }

    public boolean esParaCiudadano() {
        return this == ACEPTADA || this == RECHAZADA || this == COMPLETADA;
    }
}
