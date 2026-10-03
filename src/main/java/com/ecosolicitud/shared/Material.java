package com.ecosolicitud.shared;

public enum Material {
    PLASTICO("material.plastico", "mat-plastico"),
    CARTON("material.carton", "mat-carton"),
    PAPEL("material.papel", "mat-papel"),
    VIDRIO("material.vidrio", "mat-vidrio"),
    METAL("material.metal", "mat-metal"),
    ELECTRONICOS("material.electronicos", "mat-electronicos");

    private final String clave;
    private final String claseCss;

    Material(String clave, String claseCss) {
        this.clave = clave;
        this.claseCss = claseCss;
    }

    public String getClave() {
        return clave;
    }

    public String getClaseCss() {
        return claseCss;
    }
}
