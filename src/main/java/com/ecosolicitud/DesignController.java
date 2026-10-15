package com.ecosolicitud;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ecosolicitud.comunidad.ComunidadService;
import com.ecosolicitud.guia.GuiaService;

// /design dibuja la portada esqueleto por ciudad; el contenido vive en DesignDatos.
@Controller
class DesignController {

    private final ComunidadService comunidad;
    private final GuiaService guia;

    DesignController(ComunidadService comunidad, GuiaService guia) {
        this.comunidad = comunidad;
        this.guia = guia;
    }

    @GetMapping("/design")
    String design(@RequestParam(defaultValue = "rivera") String ciudad, Model model) {
        var pagina = DesignDatos.para(ciudad, guia);
        model.addAttribute("pagina", pagina);
        model.addAttribute("avisoEnlace", DesignDatos.enlaceAviso(pagina, comunidad));
        return "design";
    }
}
