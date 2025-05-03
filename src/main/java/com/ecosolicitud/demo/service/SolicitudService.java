package com.ecosolicitud.demo.service;

import com.ecosolicitud.demo.model.Solicitud;
import com.ecosolicitud.demo.model.Usuario;
import com.ecosolicitud.demo.repository.SolicitudRepository;
import com.ecosolicitud.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestionar solicitudes
 */
@Service
public class SolicitudService {
    
    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    
    @Autowired
    public SolicitudService(SolicitudRepository solicitudRepository, UsuarioRepository usuarioRepository) {
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        
        // Verificar si ya existen solicitudes, si no, crear algunas por defecto
        if (solicitudRepository.count() == 0) {
            usuarioRepository.findByUsername("admin").ifPresent(admin -> {
                // Agregar solicitud de ejemplo para admin
                Solicitud solicitud1 = new Solicitud();
                solicitud1.setTitulo("Solicitud de permiso");
                solicitud1.setDescripcion("Necesito permiso para ausentarme el viernes");
                solicitud1.setEstado("PENDIENTE");
                solicitud1.setFechaCreacion(LocalDateTime.now());
                solicitud1.setFechaActualizacion(LocalDateTime.now());
                solicitud1.setUsuario(admin);
                solicitudRepository.save(solicitud1);
            });
            
            usuarioRepository.findByUsername("usuario").ifPresent(usuario -> {
                // Agregar solicitud de ejemplo para usuario normal
                Solicitud solicitud2 = new Solicitud();
                solicitud2.setTitulo("Solicitud de materiales");
                solicitud2.setDescripcion("Solicito materiales para el proyecto eco-amigable");
                solicitud2.setEstado("APROBADA");
                solicitud2.setFechaCreacion(LocalDateTime.now().minusDays(5));
                solicitud2.setFechaActualizacion(LocalDateTime.now().minusDays(3));
                solicitud2.setUsuario(usuario);
                solicitudRepository.save(solicitud2);
            });
        }
    }
    
    // Método para obtener todas las solicitudes
    public List<Solicitud> findAll() {
        return solicitudRepository.findAll();
    }
    
    // Método para buscar una solicitud por ID
    public Optional<Solicitud> findById(Long id) {
        return solicitudRepository.findById(id);
    }
    
    // Método para guardar una solicitud (crear o actualizar)
    public Solicitud save(Solicitud solicitud) {
        if (solicitud.getId() == null) {
            // Es una nueva solicitud
            solicitud.setFechaCreacion(LocalDateTime.now());
            solicitud.setFechaActualizacion(LocalDateTime.now());
        } else {
            // Es una actualización
            solicitud.setFechaActualizacion(LocalDateTime.now());
        }
        
        return solicitudRepository.save(solicitud);
    }
    
    // Método para eliminar una solicitud
    public void deleteById(Long id) {
        solicitudRepository.deleteById(id);
    }
    
    // Método para buscar solicitudes por usuario
    public List<Solicitud> findByUsuario(Usuario usuario) {
        return solicitudRepository.findByUsuario(usuario);
    }
}
