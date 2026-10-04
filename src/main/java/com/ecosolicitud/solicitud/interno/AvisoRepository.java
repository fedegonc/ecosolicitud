package com.ecosolicitud.solicitud.interno;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AvisoRepository extends JpaRepository<Aviso, Long> {

    List<Aviso> findByDestinoOrderByCreadaEnDesc(String destino);

    List<Aviso> findByDestinoAndLeidaFalse(String destino);

    long countByDestinoAndLeidaFalse(String destino);
}
