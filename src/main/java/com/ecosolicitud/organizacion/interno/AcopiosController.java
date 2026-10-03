package com.ecosolicitud.organizacion.interno;

import com.ecosolicitud.Rutas;
import com.ecosolicitud.organizacion.OrganizacionService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class AcopiosController {

    private final OrganizacionService servicio;

    AcopiosController(OrganizacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping(Rutas.ACOPIOS)
    String acopios(Model modelo) {
        modelo.addAttribute("grupos", servicio.porCiudad());
        return "secciones/acopios";
    }
}
