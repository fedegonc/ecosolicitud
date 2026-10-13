package com.ecosolicitud.solicitud.interno;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import com.ecosolicitud.solicitud.Estado;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    Page<Solicitud> findByCiudadano_IdAndEstadoInOrderByCreadaEnDesc(
            String ciudadanoId, Collection<Estado> estados, Pageable pageable);

    Page<Solicitud> findByOrganizacionIdAndEstadoInOrderByCreadaEnDesc(
            String organizacionId, Collection<Estado> estados, Pageable pageable);

    List<Solicitud> findByCiudadano_IdAndCreadaEnGreaterThanEqualOrderByCreadaEnDesc(
            String ciudadanoId, Instant desde);

    List<Solicitud> findByOrganizacionIdAndCreadaEnGreaterThanEqualOrderByCreadaEnDesc(
            String organizacionId, Instant desde);

    List<Solicitud> findByCiudadano_IdAndCreadaEnGreaterThanEqualAndCreadaEnLessThanOrderByCreadaEnDesc(
            String ciudadanoId, Instant desde, Instant hasta);

    List<Solicitud> findByOrganizacionIdAndCreadaEnGreaterThanEqualAndCreadaEnLessThanOrderByCreadaEnDesc(
            String organizacionId, Instant desde, Instant hasta);

    List<Solicitud> findByCreadaEnGreaterThanEqual(Instant desde);

    long countByCiudadano_IdAndEstadoIn(String ciudadanoId, Collection<Estado> estados);

    long countByOrganizacionIdAndEstadoIn(String organizacionId, Collection<Estado> estados);
}
