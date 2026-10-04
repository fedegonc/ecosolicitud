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
    private final String tema;

    NavegacionAdvice(ActorSesion actor, OrganizacionService organizaciones,
            @Value("${ecosolicitud.demo.habilitada:true}") boolean demoHabilitada,
            @Value("${ecosolicitud.tema:}") String tema) {
        this.actor = actor;
        this.organizaciones = organizaciones;
        this.demoHabilitada = demoHabilitada;
        this.tema = tema;
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
        return actor.get() == Rol.ORGANIZACION ? organizaciones.todas() : List.of();
    }

    @ModelAttribute("organizacionActiva")
    OrganizacionInfo organizacionActiva() {
        return actor.get() == Rol.ORGANIZACION ? organizaciones.actual(actor.actual()) : null;
    }

    @ModelAttribute("tema")
    String tema() {
        return tema;
    }

    @ModelAttribute("demoHabilitada")
    boolean demoHabilitada() {
        return demoHabilitada;
    }
}
