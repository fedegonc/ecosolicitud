package com.ecosolicitud;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ecosolicitud.comunidad.ComunidadService;

// /design dibuja la portada esqueleto por ciudad; el contenido vive en DesignDatos.
@Controller
class DesignController {

    private final ComunidadService comunidad;

    DesignController(ComunidadService comunidad) {
        this.comunidad = comunidad;
    }

    @GetMapping("/design")
    String design(@RequestParam(defaultValue = "rivera") String ciudad, Model model) {
        var pagina = DesignDatos.para(ciudad);
        model.addAttribute("pagina", pagina);
        model.addAttribute("avisoEnlace", DesignDatos.enlaceAviso(pagina, comunidad));
        return "design";
    }
}
