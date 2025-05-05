package com.ecosolicitud.demo.roles.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ecosolicitud.demo.roles.application.RolService;

@Controller
@RequestMapping("/roles")
public class RolController {
    
    private final RolService rolService;
    
    @Autowired
    public RolController(RolService rolService) {
        this.rolService = rolService;
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String listarRoles(Model model) {
        model.addAttribute("roles", rolService.obtenerTodos());
        return "roles/lista";
    }
}
