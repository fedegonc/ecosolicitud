package com.ecosolicitud.shared;

public enum Material {
    PLASTICO("material.plastico"),
    CARTON("material.carton"),
    PAPEL("material.papel"),
    VIDRIO("material.vidrio"),
    METAL("material.metal"),
    ELECTRONICOS("material.electronicos");

    private final String clave;

    Material(String clave) {
        this.clave = clave;
    }

    public String getClave() {
        return clave;
    }
}
