package com.ecosolicitud.opinion.interno;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OpinionRepository extends JpaRepository<Opinion, Long> {

    @Query("select avg(o.valor) from Opinion o")
    Double promedio();

    @Query("select o.seccion, avg(o.valor) from Opinion o group by o.seccion")
    List<Object[]> promedioPorSeccion();
}
