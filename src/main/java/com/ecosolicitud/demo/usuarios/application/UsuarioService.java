package com.ecosolicitud.demo.usuarios.application;

import com.ecosolicitud.demo.usuarios.domain.Usuario;
import com.ecosolicitud.demo.usuarios.domain.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

/**
 * Servicio para gestionar usuarios
 */
@Service
public class UsuarioService {
    
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    // Método para obtener todos los usuarios
    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }
    
    // Método para buscar un usuario por ID
    public Optional<Usuario> findById(Long id) {
        return usuarioRepository.findById(id);
    }
    
    // Método para buscar un usuario por nombre de usuario
    public Optional<Usuario> findByUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }
    
    // Método para guardar un usuario (crear o actualizar)
    @Transactional
    public Usuario save(Usuario usuario) {
        // Si es un nuevo usuario o se está actualizando la contraseña
        if (usuario.getId() == null ||
            (usuario.getId() != null && usuario.getPassword() != null && !usuario.getPassword().startsWith("$2a$"))) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        } else if (usuario.getId() != null && (usuario.getPassword() == null || usuario.getPassword().isEmpty())) {
            // Si se está actualizando un usuario pero no se proporciona contraseña, mantener la existente
            usuarioRepository.findById(usuario.getId())
                .ifPresent(u -> usuario.setPassword(u.getPassword()));
        }
        
        return usuarioRepository.save(usuario);
    }
    
    @Transactional
    public void deleteById(Long id) {
        usuarioRepository.deleteById(id);
    }
    
    /**
     * Obtiene la autenticación actual del contexto de seguridad
     * @return Authentication objeto con la información del usuario autenticado
     */
    public Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
    
    /**
     * Obtiene las estadísticas del usuario actual
     * @return Map con las estadísticas del usuario
     */
    public Map<String, Integer> getUserStatistics() {
        Map<String, Integer> statistics = new HashMap<>();
        // Aquí se implementaría la lógica real para obtener estadísticas
        // Por ahora, usamos valores simulados como en el controlador
        statistics.put("totalSolicitudes", 0);
        statistics.put("solicitudesAprobadas", 0);
        statistics.put("solicitudesPendientes", 0);
        
        return statistics;
    }
}
