package com.ecosolicitud;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import com.ecosolicitud.comunidad.ComunidadService;
import com.ecosolicitud.guia.GuiaService;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.shared.Seccion;

// La puerta de entrada del vecino: una respuesta arriba y las preguntas frecuentes.
// La organización no tiene portada: entra directo a sus pedidos.
@Controller
class PortadaController {

    private final ComunidadService comunidad;
    private final GuiaService guia;
    private final ActorSesion actor;

    PortadaController(ComunidadService comunidad, GuiaService guia, ActorSesion actor) {
        this.comunidad = comunidad;
        this.guia = guia;
        this.actor = actor;
    }

    @GetMapping(Rutas.RAIZ)
    String portada(@RequestParam(required = false) String ciudad, Model modelo) {
        if (actor.get() == Rol.ORGANIZACION) {
            return "redirect:" + Seccion.inicioDe(Rol.ORGANIZACION);
        }
        var pagina = DesignDatos.para(ciudad, guia);
        modelo.addAttribute("pagina", pagina);
        modelo.addAttribute("avisoEnlace", DesignDatos.enlaceAviso(pagina, comunidad));
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
