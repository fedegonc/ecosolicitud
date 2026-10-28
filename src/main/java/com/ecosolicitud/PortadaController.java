package com.ecosolicitud;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import com.ecosolicitud.guia.GuiaService;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.solicitud.Filtro;
import com.ecosolicitud.solicitud.ListadoService;

import jakarta.servlet.http.HttpServletRequest;

// La puerta de entrada: saludo, qué se puede hacer, el último pedido y tres consejos.
@Controller
class PortadaController {

    private final GuiaService guia;
    private final ListadoService listados;
    private final OrganizacionService organizaciones;
    private final ActorSesion actor;

    PortadaController(GuiaService guia, ListadoService listados,
            OrganizacionService organizaciones, ActorSesion actor) {
        this.guia = guia;
        this.listados = listados;
        this.organizaciones = organizaciones;
        this.actor = actor;
    }

    @GetMapping(Rutas.RAIZ)
    String portada(@RequestParam(required = false) String ciudad,
            HttpServletRequest request, Model modelo) {
        var sesion = actor.actual();
        var autenticado = request.isUserInRole("CIUDADANO")
                || request.isUserInRole("ORGANIZACION")
                || request.isUserInRole("DEMO");
        if (autenticado && sesion.rol() == Rol.CIUDADANO) {
            modelo.addAttribute("usuario", sesion.nombreCiudadano());
            listados.misSolicitudes(sesion.ciudadanoId(), Filtro.TODAS, 1).stream()
                    .findFirst().ifPresent(s -> modelo.addAttribute("ultimo", s));
        }
        var local = localidad(ciudad);
        modelo.addAttribute("pagina", DesignDatos.para(local, sesion.rol(), guia));
        modelo.addAttribute("centros", organizaciones.todas().stream()
                .filter(o -> o.ciudad() != null
                        && o.ciudad().name().equals(local.name()))
                .toList());
        return "portada";
    }

    // sin ?ciudad= el centro ve la suya; el vecino, la portada por defecto
    private Localidad localidad(String ciudad) {
        if (ciudad == null && actor.get() == Rol.ORGANIZACION) {
            return Localidad.desde(organizaciones.actual(actor.actual()).ciudad().name());
        }
        return Localidad.desde(ciudad);
    }

    // /design era la maqueta: queda como redirección permanente a la portada
    // conservando ?ciudad=, ?lang= y cualquier otro parámetro
    @GetMapping("/design")
    RedirectView design() {
        var vista = new RedirectView(Rutas.RAIZ);
        vista.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        vista.setPropagateQueryParams(true);
        return vista;
    }
}
