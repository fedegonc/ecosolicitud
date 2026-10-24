package com.ecosolicitud;

import java.util.List;

import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.shared.Rutas;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// Tutorial estático: cada paso es una captura de la app (static/img/ayuda/
// {imagen}-800.webp y -1600.webp) con su texto en messages y un enlace
// a la pantalla que muestra. El recorrido depende del rol activo.
@Controller
class AyudaController {

    private static final List<Paso> CIUDADANO = List.of(
            new Paso("ciudadano-centro", Rutas.ACOPIOS),
            new Paso("ciudadano-pedir", Rutas.NUEVA),
            new Paso("ciudadano-seguir", Rutas.MIS_SOLICITUDES),
            new Paso("ciudadano-avisos", Rutas.AVISOS));

    private static final List<Paso> ORGANIZACION = List.of(
            new Paso("org-bandeja", Rutas.ORG_SOLICITUDES),
            new Paso("org-perfil", Rutas.ORG_PERFIL),
            new Paso("org-informe", Rutas.ORG_INFORME));

    private final ActorSesion actor;

    AyudaController(ActorSesion actor) {
        this.actor = actor;
    }

    @GetMapping(Rutas.AYUDA)
    String ayuda(Model modelo) {
        modelo.addAttribute("pasos", actor.get() == Rol.ORGANIZACION ? ORGANIZACION : CIUDADANO);
        return "secciones/ayuda";
    }

    record Paso(String imagen, String enlace) {
    }
}
