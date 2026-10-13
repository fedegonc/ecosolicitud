package com.ecosolicitud.solicitud;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum Filtro {
    TODAS,
    ABIERTAS,
    CERRADAS;

    public boolean muestra(Estado estado) {
        return switch (this) {
            case TODAS -> true;
            case ABIERTAS -> !estado.esFinal();
            case CERRADAS -> estado.esFinal();
        };
    }

    // el filtro como predicado de base: mismo criterio que muestra(), un IN
    public Set<Estado> estados() {
        return EnumSet.allOf(Estado.class).stream().filter(this::muestra)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Filtro desde(String valor) {
        if (valor == null) {
            return TODAS;
        }
        try {
            return valueOf(valor.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TODAS;
        }
    }
}
