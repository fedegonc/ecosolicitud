package com.ecosolicitud.solicitud;

public enum Estado {
    PENDIENTE("estado.pendiente", "reloj"),
    EN_CURSO("estado.en-curso", "camion"),
    COMPLETADA("estado.completada", "tilde"),
    RECHAZADA("estado.rechazada", "cruz"),
    CANCELADA("estado.cancelada", "prohibido");

    private final String clave;
    private final String icono;

    Estado(String clave, String icono) {
        this.clave = clave;
        this.icono = icono;
    }

    public String getClave() {
        return clave;
    }

    public String getIcono() {
        return icono;
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
