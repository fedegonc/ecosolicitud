package com.ecosolicitud.shared.interno;

import com.ecosolicitud.shared.CatalogoMateriales;
import com.ecosolicitud.shared.Material;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

// Los formularios mandan el código ("PLASTICO"); un código inexistente o
// inactivo falla el binding → 400, igual que un valor de enum desconocido.
@Component
public class CodigoMaterialConverter implements Converter<String, Material> {

    private final CatalogoMateriales catalogo;

    public CodigoMaterialConverter(CatalogoMateriales catalogo) {
        this.catalogo = catalogo;
    }

    @Override
    public Material convert(String codigo) {
        var material = catalogo.resolver(codigo.trim());
        if (!material.isActivo()) {
            throw new IllegalArgumentException("Material inactivo: " + codigo);
        }
        return material;
    }
}
