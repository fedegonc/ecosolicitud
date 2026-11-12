package com.ecosolicitud;

import com.ecosolicitud.shared.Rutas;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class ModoController {

    @GetMapping(Rutas.MODO_DEV)
    String panel(HttpServletRequest request, Model modelo) {
        modelo.addAttribute("esAdmin", request.isUserInRole("ADMIN"));
        return "secciones/modo";
    }
}
