package com.ecosolicitud.solicitud.interno;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Miga;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.solicitud.SolicitudService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class OrgSolicitudesController {

    private final SolicitudService servicio;
    private final OrganizacionService organizaciones;
    private final ActorSesion actor;

    OrgSolicitudesController(SolicitudService servicio,
            OrganizacionService organizaciones, ActorSesion actor) {
        this.servicio = servicio;
        this.organizaciones = organizaciones;
        this.actor = actor;
    }

    @GetMapping(Rutas.ORG_SOLICITUDES)
    String lista(Model modelo) {
        modelo.addAttribute("organizacion", organizaciones.actual(actor.actual()));
        modelo.addAttribute("bandeja", servicio.recibidas(actor.actual()));
        modelo.addAttribute("semana", servicio.semana(actor.actual()));
        modelo.addAttribute("archivadas", servicio.archivadas(actor.actual()).size());
        return "secciones/org/solicitudes";
    }

    @GetMapping(Rutas.ORG_ARCHIVADAS)
    String archivadas(Model modelo) {
        modelo.addAttribute("solicitudes", servicio.archivadas(actor.actual()));
        modelo.addAttribute("migaFinal", Miga.actualClave("org.archivadas.titulo"));
        return "secciones/org/archivadas";
    }

    @GetMapping(Rutas.ORG_INFORME)
    String informe(@RequestParam(required = false) String mes, Model modelo) {
        modelo.addAttribute("titular", organizaciones.actual(actor.actual()).nombre());
        modelo.addAttribute("volver", Rutas.ORG_SOLICITUDES);
        modelo.addAttribute("rutaInforme", Rutas.ORG_INFORME);
        modelo.addAttribute("migaFinal", Miga.actualClave("panel.informe"));
        modelo.addAttribute("informe", servicio.informe(actor.actual(), parseMes(mes)));
        return "secciones/informe";
    }

    // con HX-Request devuelve solo el detalle expandible; si no, la página
    @GetMapping(Rutas.ORG_SOLICITUDES + "/{id}")
    String detalle(@PathVariable long id, Model modelo,
            @RequestHeader(name = "HX-Request", required = false) boolean htmx) {
        var s = servicio.detalle(actor.actual(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        modelo.addAttribute("s", s);
        modelo.addAttribute("titular", s.nombreCiudadano());
        modelo.addAttribute("volver", Rutas.ORG_SOLICITUDES);
        modelo.addAttribute("migaFinal", Miga.actual("#" + s.id()));
        return htmx ? "secciones/ficha :: detalle" : "secciones/solicitud";
    }

    @PostMapping(Rutas.ORG_SOLICITUDES + "/{id}/aceptar")
    String aceptar(@PathVariable long id, @RequestParam long version,
            RedirectAttributes redir) {
        return Respuestas.intentar(() -> servicio.aceptar(actor.actual(), id, version),
                "org.aceptada", id, Rutas.ORG_SOLICITUDES, redir);
    }

    @PostMapping(Rutas.ORG_SOLICITUDES + "/{id}/rechazar")
    String rechazar(@PathVariable long id, @RequestParam long version,
            RedirectAttributes redir) {
        return Respuestas.intentar(() -> servicio.rechazar(actor.actual(), id, version),
                "org.rechazada", id, Rutas.ORG_SOLICITUDES, redir);
    }

    @PostMapping(Rutas.ORG_SOLICITUDES + "/{id}/completar")
    String completar(@PathVariable long id, @RequestParam long version,
            RedirectAttributes redir) {
        return Respuestas.intentar(() -> servicio.completar(actor.actual(), id, version),
                "org.completada", id, Rutas.ORG_SOLICITUDES, redir);
    }

    private static YearMonth parseMes(String mes) {
        try {
            return mes == null ? YearMonth.now() : YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            return YearMonth.now();
        }
    }
}
