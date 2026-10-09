package com.ecosolicitud.solicitud;

public enum Estado {
    PENDIENTE("estado.pendiente", "reloj", "estado.ayuda.pendiente"),
    EN_CURSO("estado.en-curso", "camion", "estado.ayuda.en-curso"),
    COMPLETADA("estado.completada", "tilde", "estado.ayuda.completada"),
    RECHAZADA("estado.rechazada", "cruz", "estado.ayuda.rechazada"),
    CANCELADA("estado.cancelada", "prohibido", "estado.ayuda.cancelada");

    private final String clave;
    private final String icono;
    private final String ayuda;

    Estado(String clave, String icono, String ayuda) {
        this.clave = clave;
        this.icono = icono;
        this.ayuda = ayuda;
    }

    public String getClave() {
        return clave;
    }

    public String getIcono() {
        return icono;
    }

    public String getAyuda() {
        return ayuda;
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
