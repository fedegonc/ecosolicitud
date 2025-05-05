package com.ecosolicitud.demo.usuarios.api;

import com.ecosolicitud.demo.roles.domain.Rol;
import com.ecosolicitud.demo.roles.domain.RolRepository;
import com.ecosolicitud.demo.usuarios.domain.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Mapeador para convertir entre entidades de dominio y DTOs
 */
@Component
public class UsuarioMapper {
    
    @Autowired
    private RolRepository rolRepository;
    
    /**
     * Convierte una entidad Usuario a un DTO
     */
    public UsuarioDTO toDto(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setApellido(usuario.getApellido());
        dto.setEmail(usuario.getEmail());
        dto.setUsername(usuario.getUsername());
        // No transferimos la contraseña al DTO por seguridad
        
        // Convertimos el objeto Rol a String (nombre del rol)
        if (usuario.getRol() != null) {
            dto.setRol(usuario.getRol().getNombre());
        }
        
        return dto;
    }
    
    /**
     * Convierte un DTO a una entidad Usuario
     */
    public Usuario toEntity(UsuarioDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Usuario usuario = new Usuario();
        usuario.setId(dto.getId());
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setEmail(dto.getEmail());
        usuario.setUsername(dto.getUsername());
        usuario.setPassword(dto.getPassword());
        
        // Convertimos el String (nombre del rol) a objeto Rol
        if (dto.getRol() != null && !dto.getRol().isEmpty()) {
            rolRepository.findByNombre(dto.getRol())
                .ifPresent(usuario::setRol);
        }
        
        return usuario;
    }
    
    /**
     * Actualiza una entidad existente con datos del DTO
     */
    public void updateEntityFromDto(UsuarioDTO dto, Usuario usuario) {
        if (dto == null || usuario == null) {
            return;
        }
        
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setEmail(dto.getEmail());
        usuario.setUsername(dto.getUsername());
        
        // Solo actualizamos la contraseña si se proporciona una nueva
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            usuario.setPassword(dto.getPassword());
        }
        
        // Convertimos el String (nombre del rol) a objeto Rol
        if (dto.getRol() != null && !dto.getRol().isEmpty()) {
            rolRepository.findByNombre(dto.getRol())
                .ifPresent(usuario::setRol);
        }
    }
}
