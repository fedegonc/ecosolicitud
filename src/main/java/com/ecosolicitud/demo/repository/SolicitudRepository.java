package com.ecosolicitud.demo.repository;

import com.ecosolicitud.demo.model.Solicitud;
import com.ecosolicitud.demo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    List<Solicitud> findByUsuario(Usuario usuario);
}
