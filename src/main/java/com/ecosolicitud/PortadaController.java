package com.ecosolicitud;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import com.ecosolicitud.shared.Rutas;

// La puerta de entrada: qué es el servicio y el primer paso, con lo último de la comunidad.
@Controller
class PortadaController {

    @GetMapping(Rutas.RAIZ)
    String portada(@RequestParam(defaultValue = "rivera") String ciudad, Model modelo) {
        modelo.addAttribute("pagina", DesignDatos.para(ciudad));
        modelo.addAttribute("ultimas", List.of("novo-horizonte", "rivera-verde", "punto-verde"));
        return "portada";
    }
}
