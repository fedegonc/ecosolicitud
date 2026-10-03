package com.ecosolicitud;

import jakarta.servlet.http.HttpServletRequest;

import com.ecosolicitud.shared.ActorSesion;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class EstructuraProvisionalController {

    private final ActorSesion actor;

    EstructuraProvisionalController(ActorSesion actor) {
        this.actor = actor;
    }

    @GetMapping(Rutas.RAIZ)
    String raiz() {
        return "redirect:" + Seccion.inicioDe(actor.get()).getRuta();
    }

    @GetMapping({Rutas.INICIO, Rutas.NUEVA, Rutas.MIS_SOLICITUDES,
            Rutas.NOTIFICACIONES, Rutas.PERFIL, Rutas.ORG_PANEL, Rutas.ORG_SOLICITUDES,
            Rutas.ORG_REPORTE})
    String seccion(HttpServletRequest request) {
        return "secciones" + Seccion.desdeRuta(request.getRequestURI()).getRuta();
    }
}
