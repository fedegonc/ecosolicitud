package com.ecosolicitud.solicitud.interno;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.solicitud.AvisoService;

// Bandeja de avisos del actor activo; verla marca todo como leído.
@Controller
class AvisosController {

    private final AvisoService servicio;
    private final ActorSesion actor;

    AvisosController(AvisoService servicio, ActorSesion actor) {
        this.servicio = servicio;
        this.actor = actor;
    }

    @GetMapping(Rutas.AVISOS)
    String avisos(Model modelo) {
        var a = actor.actual();
        modelo.addAttribute("avisos", servicio.avisosPara(a));
        modelo.addAttribute("columnas", servicio.kanbanPara(a));
        servicio.marcarLeidas(a);
        return "secciones/avisos";
    }

    // solo el badge: el nav lo pide periódicamente sin navegar
    @GetMapping(Rutas.AVISOS + "/contador")
    String contador() {
        return "secciones/badge :: badge";
    }
}
