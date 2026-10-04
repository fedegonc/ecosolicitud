package com.ecosolicitud.guia;

import com.ecosolicitud.shared.Material;

public record ArticuloInfo(long id, String titulo, String cuerpo, Material material,
        int orden) {
}
