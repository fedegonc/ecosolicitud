package com.ecosolicitud.cuenta.interno;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuariosRepository extends JpaRepository<UsuarioEntidad, String> {

    Optional<UsuarioEntidad> findByTelefono(String telefono);

    Optional<UsuarioEntidad> findByNombre(String nombre);
}
