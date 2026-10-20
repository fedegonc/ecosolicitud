package com.ecosolicitud.solicitud.interno;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Miga;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.solicitud.Filtro;
import com.ecosolicitud.solicitud.ListadoService;
import com.ecosolicitud.solicitud.SolicitudInfo;
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
class MisSolicitudesController {

    private final SolicitudService servicio;
    private final ListadoService listados;
    private final ActorSesion actor;

    MisSolicitudesController(SolicitudService servicio, ListadoService listados,
            ActorSesion actor) {
        this.servicio = servicio;
        this.listados = listados;
        this.actor = actor;
    }

    @GetMapping(Rutas.MIS_SOLICITUDES)
    String lista(@RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "1") int pagina, Model modelo) {
        var activo = Filtro.desde(filtro);
        modelo.addAttribute("filtro", activo);
        modelo.addAttribute("filtros", Filtro.values());
        modelo.addAttribute("pagina", listados.misSolicitudes(
                actor.actual().ciudadanoId(), activo, pagina));
        modelo.addAttribute("usuario", actor.actual().nombreCiudadano());
        modelo.addAttribute("semana", listados.semana(actor.actual()));
        modelo.addAttribute("archivadas", listados.totalArchivadas(actor.actual()));
        return "secciones/mis-solicitudes";
    }

    @GetMapping(Rutas.MIS_INFORME)
    String informe(@RequestParam(required = false) String mes, Model modelo) {
        modelo.addAttribute("titular", actor.actual().nombreCiudadano());
        modelo.addAttribute("volver", Rutas.MIS_SOLICITUDES);
        modelo.addAttribute("rutaInforme", Rutas.MIS_INFORME);
        modelo.addAttribute("migaFinal", Miga.actualClave("panel.informe"));
        modelo.addAttribute("informe", listados.informe(actor.actual(), parseMes(mes)));
        return "secciones/informe";
    }

    // el enlace "Detalles" (hx-get) recibe solo el detalle expandible; una
    // navegación boosteada (hx-boost también manda HX-Request) recibe la página
    @GetMapping(Rutas.MIS_SOLICITUD)
    String detalle(@PathVariable long id, Model modelo,
            @RequestHeader(name = "HX-Request", required = false) boolean htmx,
            @RequestHeader(name = "HX-Boosted", required = false) boolean boosted) {
        var s = solicitudDelActor(id);
        modelo.addAttribute("s", s);
        modelo.addAttribute("titular", s.organizacionNombre());
        modelo.addAttribute("volver", Rutas.MIS_SOLICITUDES);
        modelo.addAttribute("migaFinal", Miga.actual("#" + s.id()));
        return htmx && !boosted ? "secciones/ficha :: detalle" : "secciones/solicitud";
    }

    @PostMapping(Rutas.MIS_CANCELAR)
    String cancelar(@PathVariable long id, @RequestParam long version,
            RedirectAttributes redir) {
        return Respuestas.intentar(() -> servicio.cancelar(actor.actual(), id, version),
                "mis.cancelada", id, Rutas.MIS_SOLICITUDES, redir);
    }

    private SolicitudInfo solicitudDelActor(long id) {
        return servicio.detalle(actor.actual(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static YearMonth parseMes(String mes) {
        try {
            return mes == null ? YearMonth.now() : YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            return YearMonth.now();
        }
    }
}
