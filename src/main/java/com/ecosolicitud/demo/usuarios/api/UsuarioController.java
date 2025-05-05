package com.ecosolicitud.demo.usuarios.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ecosolicitud.demo.usuarios.application.UsuarioService;

import java.util.Map;

@Controller
public class UsuarioController {

    private final UsuarioService usuarioService;
    
    @Autowired
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping(value = {"/", "/index"})
    public String index() {
        return "usuarios/index";
    }

    @GetMapping("/home")
    public String home(Model model) {
        // Utilizamos el servicio para obtener las estadísticas
        Map<String, Integer> statistics = usuarioService.getUserStatistics();
        
        // Añadimos las estadísticas al modelo
        model.addAllAttributes(statistics);
        
        return "usuarios/home";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error, 
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Credenciales inválidas. Por favor, inténtalo de nuevo.");
        }
        
        if (logout != null) {
            model.addAttribute("message", "Has cerrado sesión correctamente.");
        }
        
        return "usuarios/login";
    }
    
    @GetMapping("/demo")
    public String demoPage() {
        return "usuarios/demo";
    }
}
