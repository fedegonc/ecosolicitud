package com.ecosolicitud.solicitud.interno;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Actor;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Fechas;
import com.ecosolicitud.shared.Miga;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.solicitud.ListadoService;
import com.ecosolicitud.solicitud.Resultado;
import com.ecosolicitud.solicitud.SolicitudService;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.FragmentsRendering;

@Controller
class OrgSolicitudesController {

    private final SolicitudService servicio;
    private final ListadoService listados;
    private final OrganizacionService organizaciones;
    private final ActorSesion actor;

    OrgSolicitudesController(SolicitudService servicio, ListadoService listados,
            OrganizacionService organizaciones, ActorSesion actor) {
        this.servicio = servicio;
        this.listados = listados;
        this.organizaciones = organizaciones;
        this.actor = actor;
    }

    @GetMapping(Rutas.ORG_SOLICITUDES)
    String lista(Model modelo) {
        modelo.addAttribute("organizacion", organizaciones.actual(actor.actual()));
        modelo.addAttribute("bandeja", listados.recibidas(actor.actual()));
        modelo.addAttribute("semana", listados.semana(actor.actual()));
        modelo.addAttribute("archivadas", listados.totalArchivadas(actor.actual()));
        return "secciones/org/solicitudes";
    }

    @GetMapping(Rutas.ORG_ARCHIVADAS)
    String archivadas(@RequestParam(defaultValue = "1") int pagina, Model modelo) {
        modelo.addAttribute("pagina", listados.archivadas(actor.actual(), pagina));
        modelo.addAttribute("migaFinal", Miga.actualClave("org.archivadas.titulo"));
        return "secciones/org/archivadas";
    }

    @GetMapping(Rutas.ORG_INFORME)
    String informe(@RequestParam(required = false) String mes, Model modelo) {
        modelo.addAttribute("titular", organizaciones.actual(actor.actual()).nombre());
        modelo.addAttribute("volver", Rutas.ORG_SOLICITUDES);
        modelo.addAttribute("rutaInforme", Rutas.ORG_INFORME);
        modelo.addAttribute("migaFinal", Miga.actualClave("panel.informe"));
        modelo.addAttribute("informe", listados.informe(actor.actual(), parseMes(mes)));
        return "secciones/informe";
    }

    // copia de respaldo en .txt: solo la organización de la sesión; sin una
    // válida, 403 (nunca "la primera disponible", como hace actual())
    @GetMapping(Rutas.ORG_COPIA)
    ResponseEntity<String> copia() {
        var org = Optional.of(actor.actual()).filter(Actor::esOrganizacion)
                .map(Actor::organizacionId)
                .flatMap(organizaciones::buscar)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        var nombre = "ecosolicitud-" + LocalDate.now(Fechas.ZONA) + ".txt";
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombre).build().toString())
                .body(CopiaTexto.de(org, listados.todasDe(org.id())));
    }

    // el enlace "Detalles" (hx-get) recibe solo el detalle expandible; una
    // navegación boosteada (hx-boost también manda HX-Request) recibe la página
    @GetMapping(Rutas.ORG_SOLICITUD)
    String detalle(@PathVariable long id, Model modelo,
            @RequestHeader(name = "HX-Request", required = false) boolean htmx,
            @RequestHeader(name = "HX-Boosted", required = false) boolean boosted) {
        var s = servicio.detalle(actor.actual(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        modelo.addAttribute("s", s);
        modelo.addAttribute("titular", s.nombreCiudadano());
        modelo.addAttribute("volver", Rutas.ORG_SOLICITUDES);
        modelo.addAttribute("migaFinal", Miga.actual("#" + s.id()));
        return htmx && !boosted ? "secciones/ficha :: detalle" : "secciones/solicitud";
    }

    @PostMapping(Rutas.ORG_ACEPTAR)
    Object aceptar(@PathVariable long id, @RequestParam long version,
            @RequestHeader(name = "HX-Request", required = false) boolean htmx,
            RedirectAttributes redir, Model modelo) {
        return responder(Respuestas.intentar(() -> servicio.aceptar(actor.actual(), id, version)),
                "org.aceptada", id, htmx, redir, modelo);
    }

    @PostMapping(Rutas.ORG_RECHAZAR)
    Object rechazar(@PathVariable long id, @RequestParam long version,
            @RequestHeader(name = "HX-Request", required = false) boolean htmx,
            RedirectAttributes redir, Model modelo) {
        return responder(Respuestas.intentar(() -> servicio.rechazar(actor.actual(), id, version)),
                "org.rechazada", id, htmx, redir, modelo);
    }

    @PostMapping(Rutas.ORG_COMPLETAR)
    Object completar(@PathVariable long id, @RequestParam long version,
            @RequestHeader(name = "HX-Request", required = false) boolean htmx,
            RedirectAttributes redir, Model modelo) {
        return responder(Respuestas.intentar(() -> servicio.completar(actor.actual(), id, version)),
                "org.completada", id, htmx, redir, modelo);
    }

    // htmx: la bandeja actualizada y el contador de avisos del menú llegan
    // en la misma respuesta (la tarjeta cambia de columna a la vez); sin
    // htmx el POST sigue siendo post-redirect-get con flash
    private Object responder(Resultado resultado, String codigoOk, long id,
            boolean htmx, RedirectAttributes redir, Model modelo) {
        if (!htmx) {
            return Respuestas.responder(resultado, codigoOk, id, Rutas.ORG_SOLICITUDES, redir);
        }
        Respuestas.avisar(resultado, codigoOk, id, modelo::addAttribute);
        modelo.addAttribute("bandeja", listados.recibidas(actor.actual()));
        return FragmentsRendering.with("secciones/org/solicitudes :: bandeja")
                .fragment("secciones/badge :: contador-oob")
                .build();
    }

    private static YearMonth parseMes(String mes) {
        try {
            return mes == null ? YearMonth.now() : YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            return YearMonth.now();
        }
    }
}
