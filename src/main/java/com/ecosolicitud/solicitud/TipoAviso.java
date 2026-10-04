package com.ecosolicitud.solicitud;

// Qué pasó con una solicitud, para contarlo en la bandeja de avisos.
// NUEVA y CANCELADA van para la organización; el resto para el ciudadano.
public enum TipoAviso {
    NUEVA("aviso.nueva", Estado.PENDIENTE),
    ACEPTADA("aviso.aceptada", Estado.EN_CURSO),
    RECHAZADA("aviso.rechazada", Estado.RECHAZADA),
    COMPLETADA("aviso.completada", Estado.COMPLETADA),
    CANCELADA("aviso.cancelada", Estado.CANCELADA);

    private final String clave;
    private final Estado estado;

    TipoAviso(String clave, Estado estado) {
        this.clave = clave;
        this.estado = estado;
    }

    public String getClave() {
        return clave;
    }

    // el estado que el aviso anuncia; el actual puede haber seguido avanzando
    public Estado getEstado() {
        return estado;
    }

    public boolean esParaCiudadano() {
        return this == ACEPTADA || this == RECHAZADA || this == COMPLETADA;
    }
}
