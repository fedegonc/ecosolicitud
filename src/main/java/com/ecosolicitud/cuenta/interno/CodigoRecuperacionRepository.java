package com.ecosolicitud.cuenta.interno;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Long> {

    Optional<CodigoRecuperacion> findTopByUsuarioIdAndUsadaFalseOrderByCreadaEnDesc(
            String usuarioId);

    List<CodigoRecuperacion> findByUsuarioIdAndUsadaFalse(String usuarioId);
}
