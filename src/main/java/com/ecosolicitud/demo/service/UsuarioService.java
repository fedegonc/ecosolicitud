package com.ecosolicitud.demo.service;

import com.ecosolicitud.demo.model.Usuario;
import com.ecosolicitud.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestionar usuarios
 */
@Service
public class UsuarioService {
    
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        
        // Verificar si ya existen usuarios, si no, crear algunos por defecto
        if (usuarioRepository.count() == 0) {
            // Agregar un usuario administrador
            Usuario admin = new Usuario();
            admin.setNombre("Admin");
            admin.setApellido("Admin");
            admin.setEmail("admin@example.com");
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("password"));
            admin.setRol("ADMIN");
            usuarioRepository.save(admin);
            
            // Agregar un usuario normal
            Usuario usuario = new Usuario();
            usuario.setNombre("Usuario");
            usuario.setApellido("Normal");
            usuario.setEmail("usuario@example.com");
            usuario.setUsername("usuario");
            usuario.setPassword(passwordEncoder.encode("password"));
            usuario.setRol("USUARIO");
            usuarioRepository.save(usuario);
        }
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
    
    // Método para eliminar un usuario
    public void deleteById(Long id) {
        usuarioRepository.deleteById(id);
    }
    
    // Método para buscar un usuario por nombre de usuario
    public Optional<Usuario> findByUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }
}
