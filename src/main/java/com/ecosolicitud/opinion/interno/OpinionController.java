package com.ecosolicitud.opinion.interno;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ecosolicitud.opinion.OpinionService;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Rutas;

// Formulario breve de valoración, abierto a cualquier rol.
@Controller
class OpinionController {

    private final OpinionService servicio;
    private final ActorSesion actor;

    OpinionController(OpinionService servicio, ActorSesion actor) {
        this.servicio = servicio;
        this.actor = actor;
    }

    @GetMapping(Rutas.OPINION)
    String formulario() {
        return "secciones/opinion";
    }

    @PostMapping(Rutas.OPINION)
    String registrar(@RequestParam int valor,
            @RequestParam(required = false) String comentario) {
        servicio.registrar(valor, comentario, actor.actual().rol());
        return "redirect:" + Rutas.OPINION + "?gracias";
    }
}
