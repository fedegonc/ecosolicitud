package com.ecosolicitud;

import com.ecosolicitud.shared.Rutas;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class ModoController {

    @GetMapping(Rutas.MODO_DEV)
    String panel() {
        return "secciones/modo";
    }
}
