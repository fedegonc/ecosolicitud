package com.ecosolicitud.demo.roles.application;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecosolicitud.demo.roles.domain.Rol;
import com.ecosolicitud.demo.roles.domain.RolRepository;

@Service
public class RolService {
    
    private final RolRepository rolRepository;
    
    @Autowired
    public RolService(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }
    
    /**
     * Inicializa los roles básicos del sistema si no existen
     */
    public void inicializarRolesBasicos() {
        // Verificar si ya existen roles
        if (rolRepository.count() == 0) {
            // Crear roles básicos con niveles jerárquicos
            Rol rolUser = new Rol("USER", "Usuario básico del sistema", 1);
            Rol rolAdmin = new Rol("ADMIN", "Administrador con acceso completo", 10);
            
            rolRepository.save(rolUser);
            rolRepository.save(rolAdmin);
        }
    }
    
    public List<Rol> obtenerTodos() {
        return rolRepository.findAll();
    }
    
    public Optional<Rol> obtenerPorNombre(String nombre) {
        return rolRepository.findByNombre(nombre);
    }
    
    public boolean tieneJerarquia(String rolActual, String rolRequerido) {
        Optional<Rol> rolActualOpt = obtenerPorNombre(rolActual);
        Optional<Rol> rolRequeridoOpt = obtenerPorNombre(rolRequerido);
        
        if (rolActualOpt.isPresent() && rolRequeridoOpt.isPresent()) {
            return rolActualOpt.get().tieneJerarquiaSobre(rolRequeridoOpt.get());
        }
        
        return false;
    }
}
