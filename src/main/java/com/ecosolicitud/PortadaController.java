package com.ecosolicitud;

import java.time.LocalDate;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import com.ecosolicitud.comunidad.ComunidadService;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Fechas;
import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.shared.Seccion;

import jakarta.servlet.http.HttpSession;

// La puerta de entrada del vecino: una respuesta arriba y las preguntas frecuentes.
// La organización no tiene portada: entra directo a sus pedidos.
@Controller
class PortadaController {

    // la zona elegida con ?zona=N vive en la sesión; ?zona= vacío la olvida
    private static final String ZONA = "portada.zona";

    private final ComunidadService comunidad;
    private final ActorSesion actor;
    private final MessageSource mensajes;

    PortadaController(ComunidadService comunidad, ActorSesion actor, MessageSource mensajes) {
        this.comunidad = comunidad;
        this.actor = actor;
        this.mensajes = mensajes;
    }

    @GetMapping(Rutas.RAIZ)
    String portada(@RequestParam(required = false) String ciudad,
            @RequestParam(required = false) String zona, HttpSession sesion, Model modelo, Locale locale) {
        if (actor.get() == Rol.ORGANIZACION) {
            return "redirect:" + Seccion.inicioDe(Rol.ORGANIZACION);
        }
        var pagina = DesignDatos.para(ciudad);
        if (zona != null) {
            recordarZona(sesion, pagina.localidad(), zona);
        }
        modelo.addAttribute("pagina", pagina);
        modelo.addAttribute("avisoEnlace", DesignDatos.enlaceAviso(pagina, comunidad));
        Recoleccion.zona(pagina.localidad(), (Integer) sesion.getAttribute(ZONA)).ifPresent(z -> {
            var hoy = LocalDate.now(Fechas.ZONA).getDayOfWeek();
            modelo.addAttribute("zona", z);
            modelo.addAttribute("frase", frase("portada.camion", Recoleccion.comun(z, hoy), locale));
            modelo.addAttribute("reciclables", frase("portada.reciclables", Recoleccion.reciclables(z, hoy), locale));
        });
        return "portada";
    }

    private void recordarZona(HttpSession sesion, Localidad localidad, String zona) {
        try {
            var numero = Integer.valueOf(zona);
            Recoleccion.zona(localidad, numero).ifPresentOrElse(
                    z -> sesion.setAttribute(ZONA, numero), () -> sesion.removeAttribute(ZONA));
        } catch (NumberFormatException e) {
            sesion.removeAttribute(ZONA);
        }
    }

    // "Hoy a la noche pasa el camión." / "El camión pasa el jueves, a la mañana."
    private String frase(String clave, Recoleccion.Paso paso, Locale locale) {
        var turno = mensajes.getMessage("recoleccion.turno." + paso.turno(), null, locale);
        if (paso.hoy()) {
            return mensajes.getMessage(clave + ".hoy", new Object[]{turno}, locale);
        }
        var dia = mensajes.getMessage("recoleccion.dia." + paso.dia(), null, locale);
        return mensajes.getMessage(clave + ".dia", new Object[]{dia, turno}, locale);
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
