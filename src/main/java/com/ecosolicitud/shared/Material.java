package com.ecosolicitud.shared;

public enum Material {
    PLASTICO("material.plastico", "botella"),
    CARTON("material.carton", "caja"),
    PAPEL("material.papel", "hoja"),
    VIDRIO("material.vidrio", "frasco"),
    METAL("material.metal", "lata"),
    ELECTRONICOS("material.electronicos", "chip");

    private final String clave;
    private final String icono;

    Material(String clave, String icono) {
        this.clave = clave;
        this.icono = icono;
    }

    public String getClave() {
        return clave;
    }

    public String getIcono() {
        return icono;
    }
}
