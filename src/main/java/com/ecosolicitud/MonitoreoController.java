package com.ecosolicitud;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// /salud: público, para despertar el servicio y sondeos (mínimo texto).
// /metricas: autenticado, resumen agregado sin datos personales.
@RestController
final class MonitoreoController {

    private final Metricas metricas;

    MonitoreoController(Metricas metricas) {
        this.metricas = metricas;
    }

    @GetMapping("/salud")
    Map<String, String> salud() {
        return Map.of("estado", "ok");
    }

    @GetMapping("/metricas")
    Map<String, Object> resumen() {
        return metricas.resumen();
    }
}
