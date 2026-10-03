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

    @GetMapping("/")
    String raiz() {
        return "redirect:" + Seccion.inicioDe(rolSesion.get()).getRuta();
    }

    @GetMapping({"/inicio", "/nueva", "/mis-solicitudes", "/acopios", "/notificaciones", "/perfil",
            "/org/panel", "/org/solicitudes", "/org/reporte", "/org/perfil"})
    String seccion(HttpServletRequest request) {
        return "secciones" + Seccion.desdeRuta(request.getRequestURI()).getRuta();
    }
}
