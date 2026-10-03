package com.ecosolicitud;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class DemoController {

    private final RolSesion rolSesion;

    DemoController(RolSesion rolSesion) {
        this.rolSesion = rolSesion;
    }

    @PostMapping("/demo/rol")
    String cambiarRol(@RequestParam Rol rol) {
        rolSesion.cambiar(rol);
        return "redirect:" + Seccion.inicioDe(rol).getRuta();
    }
}
