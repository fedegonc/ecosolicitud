package com.ecosolicitud;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

// Localidades de la portada: el departamento de Rivera y Livramento, enfrente.
// El slug viaja en ?ciudad=; la brasileña fija el idioma en portugués
// (IdiomaInterceptor). No es shared.Ciudad: esas son las ciudades con retiro.
public enum Localidad {
    RIVERA("Rivera", false),
    TRANQUERAS("Tranqueras", false),
    MINAS("Minas de Corrales", false),
    VICHADERO("Vichadero", false),
    LIVRAMENTO("Livramento", true);

    private final String nombre;
    private final boolean brasilena;

    Localidad(String nombre, boolean brasilena) {
        this.nombre = nombre;
        this.brasilena = brasilena;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isBrasilena() {
        return brasilena;
    }

    public String getSlug() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Locale getIdioma() {
        return Locale.forLanguageTag(brasilena ? "pt" : "es");
    }

    public static List<Localidad> todas() {
        return List.of(values());
    }

    // slug desconocido o ausente: la portada por defecto
    public static Localidad desde(String slug) {
        return Arrays.stream(values()).filter(l -> l.getSlug().equalsIgnoreCase(slug))
                .findFirst().orElse(RIVERA);
    }
}
