package com.ecosolicitud.opinion.interno;

import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Rutas;

// El cuestionario de la investigación vive en Google Forms; acá solo se
// embebe y se ofrece la copia local por si Google no carga.
@Controller
class OpinionController {

    private final ActorSesion actor;
    private final CuestionariosRespaldo cuestionarios;
    private final RespaldoService respaldo;

    OpinionController(ActorSesion actor, CuestionariosRespaldo cuestionarios,
            RespaldoService respaldo) {
        this.actor = actor;
        this.cuestionarios = cuestionarios;
        this.respaldo = respaldo;
    }

    @GetMapping(Rutas.OPINION)
    String formulario(Model modelo, Locale locale) {
        modelo.addAttribute("respaldo", cuestionarios.para(actor.get(), locale));
        return "secciones/opinion";
    }

    @PostMapping(Rutas.OPINION + "/respaldo")
    String responderRespaldo(@RequestParam MultiValueMap<String, String> params, Locale locale) {
        var rol = actor.get();
        var cuestionario = cuestionarios.para(rol, locale);
        var respuestas = cuestionario.responder(i -> params.getFirst("p" + i));
        if (respuestas.isEmpty()) {
            return "redirect:" + Rutas.OPINION + "?respaldo=incompleto#respaldo";
        }
        respaldo.registrar(rol, CuestionariosRespaldo.idioma(locale), cuestionario, respuestas.get());
        return "redirect:" + Rutas.OPINION + "?respaldo=gracias#respaldo";
    }
}
