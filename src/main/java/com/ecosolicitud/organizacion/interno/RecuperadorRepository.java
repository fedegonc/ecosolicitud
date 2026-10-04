package com.ecosolicitud.organizacion.interno;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecuperadorRepository extends JpaRepository<Recuperador, Long> {

    List<Recuperador> findByOrganizacion_IdOrderByNombre(String organizacionId);
}
