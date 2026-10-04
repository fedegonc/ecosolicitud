package com.ecosolicitud.comunidad;

// Dos tipos con el mismo molde: novedades del servicio y contenido social.
// Comparten módulo porque comparten forma; se separan por filtro, no por módulo.
public enum TipoPublicacion {
    NOVEDAD("tipo.novedad"),
    HISTORIA("tipo.historia");

    private final String clave;

    TipoPublicacion(String clave) {
        this.clave = clave;
    }

    public String getClave() {
        return clave;
    }
}
