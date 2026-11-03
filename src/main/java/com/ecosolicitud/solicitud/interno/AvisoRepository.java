package com.ecosolicitud.solicitud.interno;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvisoRepository extends JpaRepository<Aviso, Long> {

    Page<Aviso> findByDestinoOrderByCreadaEnDescIdDesc(String destino, Pageable pagina);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Aviso a set a.leida = true"
            + " where a.destino = :destino and a.id in :ids and a.leida = false")
    int marcarLeidas(@Param("destino") String destino, @Param("ids") Collection<Long> ids);

    long countByDestinoAndLeidaFalse(String destino);
}
