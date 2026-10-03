package com.ecosolicitud.solicitud.interno;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    List<Solicitud> findByCiudadanoIdOrderByCreadaEnDesc(String ciudadanoId);

    List<Solicitud> findByOrganizacionIdOrderByCreadaEnDesc(String organizacionId);
}
