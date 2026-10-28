package com.ecosolicitud.cuenta;

public enum Pais {
    UY("598"),
    BR("55");

    private final String codigo;

    Pais(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
