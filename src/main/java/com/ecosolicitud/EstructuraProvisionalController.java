package com.ecosolicitud;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class EstructuraProvisionalController {

    private final RolSesion rolSesion;

    EstructuraProvisionalController(RolSesion rolSesion) {
        this.rolSesion = rolSesion;
    }

    @GetMapping(Rutas.RAIZ)
    String raiz() {
        return "redirect:" + Seccion.inicioDe(rolSesion.get()).getRuta();
    }

    @GetMapping({Rutas.INICIO, Rutas.NUEVA, Rutas.MIS_SOLICITUDES, Rutas.ACOPIOS,
            Rutas.NOTIFICACIONES, Rutas.PERFIL, Rutas.ORG_PANEL, Rutas.ORG_SOLICITUDES,
            Rutas.ORG_REPORTE, Rutas.ORG_PERFIL})
    String seccion(HttpServletRequest request) {
        return "secciones" + Seccion.desdeRuta(request.getRequestURI()).getRuta();
    }
}
