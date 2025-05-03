package com.ecosolicitud.demo.repository;

import com.ecosolicitud.demo.model.Calendar;
import com.ecosolicitud.demo.model.Solicitud;
import com.ecosolicitud.demo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {
    List<Calendar> findByUsuario(Usuario usuario);
    List<Calendar> findBySolicitud(Solicitud solicitud);
}
