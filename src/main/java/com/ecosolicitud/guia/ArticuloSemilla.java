package com.ecosolicitud.guia;

import com.ecosolicitud.shared.Material;

// Carga del dataset: la guía es contenido editorial, no dato del ciudadano.
public record ArticuloSemilla(String titulo, String cuerpo, String imagen,
        Material material, int orden) {
}
