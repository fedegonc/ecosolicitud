package com.ecosolicitud.estadisticas.interno;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ecosolicitud.estadisticas.EstadisticasService;
import com.ecosolicitud.estadisticas.Periodo;
import com.ecosolicitud.shared.Rutas;

@Controller
class EstadisticasController {

    private final EstadisticasService servicio;

    EstadisticasController(EstadisticasService servicio) {
        this.servicio = servicio;
    }

    @GetMapping(Rutas.ESTADISTICAS)
    String resumen(@RequestParam(required = false) String periodo, Model modelo) {
        modelo.addAttribute("resumen", servicio.resumen(Periodo.desdeParametro(periodo)));
        modelo.addAttribute("periodos", Periodo.values());
        return "secciones/estadisticas";
    }
}
