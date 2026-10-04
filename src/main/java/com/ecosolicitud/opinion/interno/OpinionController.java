package com.ecosolicitud.opinion.interno;

import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ecosolicitud.opinion.OpinionService;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Rutas;

// Formulario breve de valoración, abierto a cualquier rol.
@Controller
class OpinionController {

    private final OpinionService servicio;
    private final ActorSesion actor;

    OpinionController(OpinionService servicio, ActorSesion actor) {
        this.servicio = servicio;
        this.actor = actor;
    }

    @GetMapping(Rutas.OPINION)
    String formulario(Model modelo) {
        var promedios = servicio.promedioPorSeccion();
        modelo.addAttribute("promedios", promedios);
        modelo.addAttribute("promedioGeneral", promedios.get(null));
        return "secciones/opinion";
    }

    @PostMapping(Rutas.OPINION)
    String registrar(@RequestParam MultiValueMap<String, String> params) {
        var valoraciones = new LinkedHashMap<String, Integer>();
        params.forEach((clave, valores) -> {
            if (clave.startsWith("valor_") && !valores.isEmpty()) {
                try {
                    valoraciones.put(clave.substring(6),
                            Integer.valueOf(valores.get(0)));
                } catch (NumberFormatException ignorado) {
                    // param malformado: se descarta en el servicio
                }
            }
        });
        int guardadas = servicio.registrar(valoraciones,
                params.getFirst("comentario"), actor.actual().rol());
        return "redirect:" + Rutas.OPINION + (guardadas > 0 ? "?gracias" : "");
    }
}
