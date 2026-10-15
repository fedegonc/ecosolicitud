package com.ecosolicitud;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import com.ecosolicitud.comunidad.ComunidadService;
import com.ecosolicitud.guia.GuiaService;
import com.ecosolicitud.shared.Rutas;

// La puerta de entrada: qué es el servicio y el primer paso, con lo último de la comunidad.
@Controller
class PortadaController {

    private final ComunidadService comunidad;
    private final GuiaService guia;

    PortadaController(ComunidadService comunidad, GuiaService guia) {
        this.comunidad = comunidad;
        this.guia = guia;
    }

    @GetMapping(Rutas.RAIZ)
    String portada(@RequestParam(required = false) String ciudad, Model modelo) {
        var pagina = DesignDatos.para(ciudad, guia);
        modelo.addAttribute("pagina", pagina);
        modelo.addAttribute("avisoEnlace", DesignDatos.enlaceAviso(pagina, comunidad));
        modelo.addAttribute("ultimas", comunidad.ultimas(3));
        return "portada";
    }

    // /design era la maqueta: queda como redirección permanente a la portada
    // conservando ?ciudad=, ?lang= y cualquier otro parámetro
    @GetMapping("/design")
    RedirectView design() {
        var vista = new RedirectView(Rutas.RAIZ);
        vista.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        vista.setPropagateQueryParams(true);
        return vista;
    }
}
