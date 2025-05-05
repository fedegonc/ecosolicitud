package com.ecosolicitud.demo.roles.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Inicializador de roles que se ejecuta al arrancar la aplicación
 */
@Component
public class RolInitializer implements CommandLineRunner {

    private final RolService rolService;
    
    @Autowired
    public RolInitializer(RolService rolService) {
        this.rolService = rolService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        // Inicializar roles básicos al arrancar la aplicación
        rolService.inicializarRolesBasicos();
    }
}
