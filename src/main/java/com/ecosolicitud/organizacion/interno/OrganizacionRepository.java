package com.ecosolicitud.organizacion.interno;

import java.util.List;

import com.ecosolicitud.shared.Ciudad;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizacionRepository extends JpaRepository<Organizacion, String> {

    List<Organizacion> findByCiudadOrderByNombre(Ciudad ciudad);
}
