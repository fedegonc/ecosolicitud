package com.ecosolicitud.estadisticas.interno;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ecosolicitud.estadisticas.EstadisticasService;
import com.ecosolicitud.shared.Rutas;

@Controller
class EstadisticasController {

    private final EstadisticasService servicio;

    EstadisticasController(EstadisticasService servicio) {
        this.servicio = servicio;
    }

    @GetMapping(Rutas.ESTADISTICAS)
    String resumen(Model modelo) {
        modelo.addAttribute("resumen", servicio.resumen());
        return "secciones/estadisticas";
    }
}
