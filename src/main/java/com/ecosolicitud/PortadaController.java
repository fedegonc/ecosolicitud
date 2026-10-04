package com.ecosolicitud;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ecosolicitud.comunidad.ComunidadService;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.shared.Rutas;

// La puerta de entrada: qué es el servicio y el primer paso, con lo último de la comunidad.
@Controller
class PortadaController {

    private final ComunidadService comunidad;
    private final ActorSesion actor;

    PortadaController(ComunidadService comunidad, ActorSesion actor) {
        this.comunidad = comunidad;
        this.actor = actor;
    }

    @GetMapping(Rutas.RAIZ)
    String portada(Model modelo) {
        modelo.addAttribute("ultimas", comunidad.ultimas(3));
        modelo.addAttribute("inicio", actor.get() == Rol.ORGANIZACION
                ? Rutas.ORG_SOLICITUDES : Rutas.NUEVA);
        modelo.addAttribute("cta", actor.get() == Rol.ORGANIZACION
                ? "portada.cta.org" : "portada.cta.ciu");
        return "portada";
    }
}
