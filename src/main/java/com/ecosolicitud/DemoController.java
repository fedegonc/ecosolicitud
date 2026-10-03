package com.ecosolicitud;

import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.shared.Rol;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
class DemoController {

    private final ActorSesion actor;
    private final OrganizacionService organizaciones;

    DemoController(ActorSesion actor, OrganizacionService organizaciones) {
        this.actor = actor;
        this.organizaciones = organizaciones;
    }

    @PostMapping(Rutas.DEMO_ROL)
    String cambiarRol(@RequestParam Rol rol) {
        actor.cambiar(rol);
        return "redirect:" + Seccion.inicioDe(rol);
    }

    @PostMapping(Rutas.DEMO_ORGANIZACION)
    String cambiarOrganizacion(@RequestParam String id,
            @RequestParam(required = false) String volver) {
        organizaciones.buscar(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        actor.elegirOrganizacion(id);
        return "redirect:" + Seccion.rutaSegura(volver, actor.get());
    }
}
