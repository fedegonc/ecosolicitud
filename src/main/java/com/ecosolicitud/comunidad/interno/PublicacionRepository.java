package com.ecosolicitud.comunidad.interno;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {

    List<Publicacion> findAllByOrderByPublicadaEnDesc();
}
