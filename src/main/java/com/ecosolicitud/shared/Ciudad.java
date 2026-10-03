package com.ecosolicitud.shared;

public enum Ciudad {
    RIVERA("ciudad.rivera"),
    LIVRAMENTO("ciudad.livramento");

    private final String clave;

    Ciudad(String clave) {
        this.clave = clave;
    }

    public String getClave() {
        return clave;
    }
}
