package com.ecosolicitud;

import com.ecosolicitud.demo.DemoService;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Seccion;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@ConditionalOnProperty(name = "ecosolicitud.demo.habilitada",
        havingValue = "true", matchIfMissing = true)
class DemoReinicioController {

    private final DemoService demo;
    private final ActorSesion actor;

    DemoReinicioController(DemoService demo, ActorSesion actor) {
        this.demo = demo;
        this.actor = actor;
    }

    @PostMapping(Rutas.DEMO_REINICIAR)
    String reiniciar(@RequestParam(required = false) String volver) {
        demo.reiniciar();
        return "redirect:" + Seccion.rutaSegura(volver, actor.get());
    }
}
