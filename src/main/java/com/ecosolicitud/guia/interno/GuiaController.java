package com.ecosolicitud.guia.interno;

import java.util.List;

import com.ecosolicitud.guia.GuiaService;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Miga;
import com.ecosolicitud.shared.Rutas;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
class GuiaController {

    private final GuiaService guia;
    private final OrganizacionService organizaciones;

    GuiaController(GuiaService guia, OrganizacionService organizaciones) {
        this.guia = guia;
        this.organizaciones = organizaciones;
    }

    @GetMapping(Rutas.GUIA)
    String lista(Model modelo) {
        modelo.addAttribute("articulos", guia.articulos());
        return "secciones/guia";
    }

    @GetMapping(Rutas.GUIA + "/{id}")
    String articulo(@PathVariable long id, Model modelo) {
        var articulo = guia.buscar(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        modelo.addAttribute("articulo", articulo);
        modelo.addAttribute("migaFinal", Miga.actual(articulo.titulo()));
        // la guía explica, los centros ejecutan: si el artículo es de un material,
        // se enlaza a quién lo recibe (dato que ya vive en el módulo organizacion)
        modelo.addAttribute("centros", articulo.material() == null
                ? List.of() : organizaciones.queReciben(articulo.material()));
        return "secciones/guia-articulo";
    }
}
