package com.ecosolicitud;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ecosolicitud.comunidad.ComunidadService;
import com.ecosolicitud.shared.Rutas;

// La puerta de entrada: qué es el servicio y el primer paso, con lo último de la comunidad.
@Controller
class PortadaController {

    private final ComunidadService comunidad;

    PortadaController(ComunidadService comunidad) {
        this.comunidad = comunidad;
    }

    @GetMapping(Rutas.RAIZ)
    String portada(@RequestParam(defaultValue = "rivera") String ciudad, Model modelo) {
        var pagina = DesignDatos.para(ciudad);
        modelo.addAttribute("pagina", pagina);
        modelo.addAttribute("avisoEnlace", DesignDatos.enlaceAviso(pagina, comunidad));
        modelo.addAttribute("ultimas", comunidad.ultimas(3));
        return "portada";
    }
}
