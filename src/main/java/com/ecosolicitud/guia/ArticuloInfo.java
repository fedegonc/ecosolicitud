package com.ecosolicitud.guia;

import com.ecosolicitud.shared.Material;

public record ArticuloInfo(long id, String titulo, String cuerpoHtml, String imagen,
        Material material, int orden) {
}
