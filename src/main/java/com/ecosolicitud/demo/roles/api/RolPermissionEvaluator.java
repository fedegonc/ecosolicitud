package com.ecosolicitud.demo.roles.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.ecosolicitud.demo.roles.application.RolService;

/**
 * Evaluador de permisos basado en roles para usar en Thymeleaf
 */
@Component
public class RolPermissionEvaluator {
    
    private final RolService rolService;
    
    @Autowired
    public RolPermissionEvaluator(RolService rolService) {
        this.rolService = rolService;
    }
    
    /**
     * Verifica si el usuario tiene un rol específico
     */
    public boolean tieneRol(Authentication authentication, String rol) {
        return authentication != null && 
               authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_" + rol));
    }
    
    /**
     * Verifica si el usuario tiene un rol con jerarquía suficiente
     */
    public boolean tieneJerarquia(Authentication authentication, String rolRequerido) {
        if (authentication == null) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
            .map(auth -> auth.getAuthority().replace("ROLE_", ""))
            .anyMatch(rolActual -> rolService.tieneJerarquia(rolActual, rolRequerido));
    }
}
