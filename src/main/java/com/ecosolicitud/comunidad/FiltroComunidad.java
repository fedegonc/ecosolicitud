package com.ecosolicitud.comunidad;

// Filtro de la sección: valor inválido se trata como TODAS.
public enum FiltroComunidad {
    TODAS, NOVEDAD, HISTORIA;

    public static FiltroComunidad desde(String valor) {
        try {
            return valueOf(valor.toUpperCase());
        } catch (RuntimeException e) {
            return TODAS;
        }
    }

    public boolean muestra(TipoPublicacion tipo) {
        return this == TODAS || name().equals(tipo.name());
    }
}
