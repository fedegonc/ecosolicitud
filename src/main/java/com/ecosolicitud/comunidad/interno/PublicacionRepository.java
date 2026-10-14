package com.ecosolicitud.comunidad.interno;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {

    List<Publicacion> findAllByOrderByPublicadaEnDesc();

    Optional<Publicacion> findByTitulo(String titulo);
}
