package com.ecosolicitud;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
class NavegacionAdvice {

    @ModelAttribute("rutaActual")
    String rutaActual(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("secciones")
    List<Seccion> secciones() {
        // E2: reemplazar CIUDADANO por el rol de la sesión
        return Seccion.paraRol(Rol.CIUDADANO);
    }
}
