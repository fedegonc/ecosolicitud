package com.ecosolicitud.shared.interno;

import java.util.Optional;

import com.ecosolicitud.shared.Material;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    Optional<Material> findByCodigo(String codigo);

    java.util.List<Material> findByActivoTrueOrderById();
}
