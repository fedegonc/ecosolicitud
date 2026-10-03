package com.ecosolicitud;

import java.util.List;

import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Rol;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
class NavegacionAdvice {

    private final ActorSesion actor;
    private final OrganizacionService organizaciones;
    private final boolean demoHabilitada;

    NavegacionAdvice(ActorSesion actor, OrganizacionService organizaciones,
            @Value("${ecosolicitud.demo.habilitada:true}") boolean demoHabilitada) {
        this.actor = actor;
        this.organizaciones = organizaciones;
        this.demoHabilitada = demoHabilitada;
    }

    @ModelAttribute("rutaActual")
    String rutaActual(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("rolActual")
    Rol rolActual() {
        return actor.get();
    }

    @ModelAttribute("secciones")
    List<Seccion> secciones() {
        return Seccion.paraRol(actor.get());
    }

    @ModelAttribute("organizaciones")
    List<OrganizacionInfo> organizaciones() {
        return organizaciones.todas();
    }

    @ModelAttribute("organizacionActiva")
    OrganizacionInfo organizacionActiva() {
        return organizaciones.actual(actor.actual());
    }

    @ModelAttribute("demoHabilitada")
    boolean demoHabilitada() {
        return demoHabilitada;
    }
}
