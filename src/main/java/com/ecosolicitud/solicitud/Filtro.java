package com.ecosolicitud.solicitud;

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
