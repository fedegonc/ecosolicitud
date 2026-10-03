package com.ecosolicitud.solicitud.interno;

import com.ecosolicitud.Rutas;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.solicitud.SolicitudService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class OrgSolicitudesController {

    private final SolicitudService servicio;
    private final ActorSesion actor;

    OrgSolicitudesController(SolicitudService servicio, ActorSesion actor) {
        this.servicio = servicio;
        this.actor = actor;
    }

    @GetMapping(Rutas.ORG_SOLICITUDES)
    String lista(Model modelo) {
        modelo.addAttribute("bandeja", servicio.recibidas(actor.getOrganizacionId()));
        return "secciones/org/solicitudes";
    }

    @PostMapping(Rutas.ORG_SOLICITUDES + "/{id}/aceptar")
    String aceptar(@PathVariable long id, @RequestParam long version,
            RedirectAttributes redir) {
        return Respuestas.responder(servicio.aceptar(actor.actual(), id, version), "org.aceptada",
                id, Rutas.ORG_SOLICITUDES, redir);
    }

    @PostMapping(Rutas.ORG_SOLICITUDES + "/{id}/rechazar")
    String rechazar(@PathVariable long id, @RequestParam long version,
            RedirectAttributes redir) {
        return Respuestas.responder(servicio.rechazar(actor.actual(), id, version), "org.rechazada",
                id, Rutas.ORG_SOLICITUDES, redir);
    }

    @PostMapping(Rutas.ORG_SOLICITUDES + "/{id}/completar")
    String completar(@PathVariable long id, @RequestParam long version,
            RedirectAttributes redir) {
        return Respuestas.responder(servicio.completar(actor.actual(), id, version), "org.completada",
                id, Rutas.ORG_SOLICITUDES, redir);
    }
}
