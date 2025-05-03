package com.ecosolicitud.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @GetMapping("/perfil")
    public String verPerfil(Model model) {
        // En el MVP solo mostraremos una página de perfil estática
        return "usuarios/perfil";
    }
}
