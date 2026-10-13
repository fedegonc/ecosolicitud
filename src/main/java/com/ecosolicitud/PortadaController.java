package com.ecosolicitud;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.CodigoQr;
import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.shared.Rutas;

// La puerta de entrada: qué es el servicio y el primer paso, con lo último de la comunidad.
@Controller
class PortadaController {

    private final ActorSesion actor;

    PortadaController(ActorSesion actor) {
        this.actor = actor;
    }

    @GetMapping(Rutas.RAIZ)
    String portada(Model modelo) {
        modelo.addAttribute("ultimas", List.of("novo-horizonte", "rivera-verde", "punto-verde"));
        modelo.addAttribute("inicio", actor.get() == Rol.ORGANIZACION
                ? Rutas.ORG_SOLICITUDES : Rutas.NUEVA);
        modelo.addAttribute("cta", actor.get() == Rol.ORGANIZACION
                ? "portada.cta.org" : "portada.cta.ciu");
        modelo.addAttribute("qr", CodigoQr.svg(
                ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()));
        return "portada";
    }
}
