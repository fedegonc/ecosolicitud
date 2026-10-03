package com.ecosolicitud;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
class NavegacionAdvice {

    private final RolSesion rolSesion;

    NavegacionAdvice(RolSesion rolSesion) {
        this.rolSesion = rolSesion;
    }

    @ModelAttribute("rutaActual")
    String rutaActual(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("rolActual")
    Rol rolActual() {
        return rolSesion.get();
    }

    @ModelAttribute("secciones")
    List<Seccion> secciones() {
        return Seccion.paraRol(rolSesion.get());
    }
}
