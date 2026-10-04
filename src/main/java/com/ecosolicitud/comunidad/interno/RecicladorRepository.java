package com.ecosolicitud.comunidad.interno;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecicladorRepository extends JpaRepository<Reciclador, Long> {

    List<Reciclador> findAllByOrderByNombre();
}
