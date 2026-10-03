package com.ecosolicitud;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class EstructuraProvisionalController {

    @GetMapping("/")
    String raiz() {
        return "redirect:/inicio";
    }

    @GetMapping({"/inicio", "/nueva", "/mis-solicitudes", "/acopios", "/notificaciones", "/perfil",
            "/org/panel", "/org/solicitudes", "/org/materiales", "/org/reportes"})
    String seccion(HttpServletRequest request) {
        return "secciones" + Seccion.desdeRuta(request.getRequestURI()).getRuta();
    }
}
