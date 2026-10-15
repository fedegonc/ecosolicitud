package com.ecosolicitud.guia.interno;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticuloRepository extends JpaRepository<Articulo, Long> {

    List<Articulo> findAllByOrderByOrdenAsc();

    Optional<Articulo> findByTitulo(String titulo);
}
