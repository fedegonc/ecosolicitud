package com.ecosolicitud.solicitud.interno;

import com.ecosolicitud.Rutas;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.solicitud.Filtro;
import com.ecosolicitud.solicitud.SolicitudService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class MisSolicitudesController {

    private final SolicitudService servicio;
    private final ActorSesion actor;

    MisSolicitudesController(SolicitudService servicio, ActorSesion actor) {
        this.servicio = servicio;
        this.actor = actor;
    }

    @GetMapping(Rutas.MIS_SOLICITUDES)
    String lista(@RequestParam(required = false) String filtro, Model modelo) {
        var activo = Filtro.desde(filtro);
        modelo.addAttribute("filtro", activo);
        modelo.addAttribute("filtros", Filtro.values());
        modelo.addAttribute("solicitudes",
                servicio.misSolicitudes(actor.actual().ciudadanoId(), activo));
        return "secciones/mis-solicitudes";
    }

    @PostMapping(Rutas.MIS_SOLICITUDES + "/{id}/cancelar")
    String cancelar(@PathVariable long id, @RequestParam long version,
            RedirectAttributes redir) {
        return Respuestas.responder(servicio.cancelar(actor.actual(), id, version), "mis.cancelada",
                id, Rutas.MIS_SOLICITUDES, redir);
    }
}
