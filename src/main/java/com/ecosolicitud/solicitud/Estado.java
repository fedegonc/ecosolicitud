package com.ecosolicitud.solicitud;

public enum Estado {
    PENDIENTE("estado.pendiente", "est-pendiente"),
    EN_CURSO("estado.en-curso", "est-en-curso"),
    COMPLETADA("estado.completada", "est-completada"),
    RECHAZADA("estado.rechazada", "est-rechazada"),
    CANCELADA("estado.cancelada", "est-cancelada");

    private final String clave;
    private final String claseCss;

    Estado(String clave, String claseCss) {
        this.clave = clave;
        this.claseCss = claseCss;
    }

    public String getClave() {
        return clave;
    }

    public String getClaseCss() {
        return claseCss;
    }

    public boolean esFinal() {
        return this == COMPLETADA || this == RECHAZADA || this == CANCELADA;
    }
}
