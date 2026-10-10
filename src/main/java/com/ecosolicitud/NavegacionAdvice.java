package com.ecosolicitud;

import java.util.List;

import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.solicitud.AvisoService;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Miga;
import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.shared.Seccion;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
class NavegacionAdvice {

    private final ActorSesion actor;
    private final OrganizacionService organizaciones;
    private final AvisoService avisos;
    private final boolean demoHabilitada;
    private final boolean demoReinicio;
    private final String tema;

    NavegacionAdvice(ActorSesion actor, OrganizacionService organizaciones,
            AvisoService avisos,
            @Value("${ecosolicitud.demo.habilitada:true}") boolean demoHabilitada,
            @Value("${ecosolicitud.demo.reiniciar:true}") boolean demoReinicio,
            @Value("${ecosolicitud.tema:}") String tema) {
        this.actor = actor;
        this.organizaciones = organizaciones;
        this.avisos = avisos;
        this.demoHabilitada = demoHabilitada;
        this.demoReinicio = demoReinicio;
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

    // tramo fijo de la ruta: Inicio › Sección. En subrutas la sección queda
    // enlazada y el controller agrega la miga final (atributo "migaFinal").
    @ModelAttribute("migas")
    List<Miga> migas(HttpServletRequest request) {
        var ruta = request.getRequestURI();
        try {
            var s = Seccion.desdeRuta(ruta);
            return s.getRuta().equals(ruta)
                    ? List.of(Miga.inicio(), Miga.actualClave(s.getClave()))
                    : List.of(Miga.inicio(), Miga.seccion(s));
        } catch (ResponseStatusException e) {
            return List.of(ruta.equals("/") ? Miga.actualClave("miga.inicio")
                    : Miga.inicio());
        }
    }

    @ModelAttribute("avisosSinLeer")
    long avisosSinLeer() {
        return avisos.avisosSinLeer(actor.actual());
    }

    @ModelAttribute("tema")
    String tema() {
        return tema;
    }

    @ModelAttribute("demoHabilitada")
    boolean demoHabilitada() {
        return demoHabilitada;
    }

    @ModelAttribute("demoReinicio")
    boolean demoReinicio() {
        return demoReinicio;
    }
}
