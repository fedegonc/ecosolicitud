package com.ecosolicitud.solicitud;

public enum Estado {
    PENDIENTE("estado.pendiente"),
    EN_CURSO("estado.en-curso"),
    COMPLETADA("estado.completada"),
    RECHAZADA("estado.rechazada"),
    CANCELADA("estado.cancelada");

    private final String clave;

    Estado(String clave) {
        this.clave = clave;
    }

    public String getClave() {
        return clave;
    }

    public boolean esFinal() {
        return this == COMPLETADA || this == RECHAZADA || this == CANCELADA;
    }

    public boolean permiteAceptar() {
        return this == PENDIENTE;
    }

    public boolean permiteRechazar() {
        return this == PENDIENTE || this == EN_CURSO;
    }

    public boolean permiteCompletar() {
        return this == EN_CURSO;
    }

    public boolean permiteCancelar() {
        return this == PENDIENTE;
    }
}
