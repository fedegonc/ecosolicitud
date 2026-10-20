package com.ecosolicitud;

import com.ecosolicitud.shared.Rutas;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Banco de pruebas del layout: el decorador app.html con el CSS y el htmx reales,
// contenido lorem ipsum y cada caja delineada para ver solapes y anidamientos.
@Controller
@Profile("dev & !prod")
class LayoutSystemController {

    @GetMapping(Rutas.DEV_LAYOUT_SYSTEM)
    String layoutSystem(@RequestParam(defaultValue = "a") String bloque, Model modelo) {
        modelo.addAttribute("bloque", bloque);
        return "dev/layout-system";
    }
}
