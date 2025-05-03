package com.ecosolicitud.demo.service;

import com.ecosolicitud.demo.model.Usuario;
import com.ecosolicitud.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    
    // Método para guardar un usuario (crear o actualizar)
    @Transactional
    public Usuario save(Usuario usuario) {
        // Si es un nuevo usuario o se está actualizando la contraseña
        if (usuario .getId() == null ||
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
}
