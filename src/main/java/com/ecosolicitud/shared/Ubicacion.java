package com.ecosolicitud.shared;

import java.util.Locale;

public record Ubicacion(double latitud, double longitud) {

    private static final double MARGEN = 0.004;

    public Ubicacion {
        // negado: NaN falla toda comparación y con la forma directa pasaría
        if (!(latitud >= -90 && latitud <= 90 && longitud >= -180 && longitud <= 180)) {
            throw new IllegalArgumentException("Coordenadas fuera de rango");
        }
    }

    public String mapaEmbebido() {
        return String.format(Locale.ROOT,
                "https://www.openstreetmap.org/export/embed.html?bbox=%.5f,%.5f,%.5f,%.5f&layer=mapnik&marker=%.5f,%.5f",
                longitud - MARGEN, latitud - MARGEN, longitud + MARGEN, latitud + MARGEN, latitud, longitud);
    }

    public String enlaceMapa() {
        return String.format(Locale.ROOT,
                "https://www.openstreetmap.org/?mlat=%.5f&mlon=%.5f#map=17/%.5f/%.5f",
                latitud, longitud, latitud, longitud);
    }
}
