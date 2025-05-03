package com.ecosolicitud.demo.service;

import com.ecosolicitud.demo.model.Calendar;
import com.ecosolicitud.demo.model.Solicitud;
import com.ecosolicitud.demo.model.Usuario;
import com.ecosolicitud.demo.repository.CalendarRepository;
import com.ecosolicitud.demo.repository.SolicitudRepository;
import com.ecosolicitud.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestionar eventos del calendario
 */
@Service
public class CalendarService {
    
    private final CalendarRepository calendarRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudRepository solicitudRepository;
    
    @Autowired
    public CalendarService(CalendarRepository calendarRepository, 
                          UsuarioRepository usuarioRepository,
                          SolicitudRepository solicitudRepository) {
        this.calendarRepository = calendarRepository;
        this.usuarioRepository = usuarioRepository;
        this.solicitudRepository = solicitudRepository;
        
        // Verificar si ya existen eventos, si no, crear algunos por defecto
        if (calendarRepository.count() == 0) {
            usuarioRepository.findByUsername("admin").ifPresent(admin -> {
                // Agregar evento de ejemplo para admin
                Calendar evento1 = new Calendar();
                evento1.setTitulo("Reunión de equipo");
                evento1.setDescripcion("Reunión semanal del equipo de desarrollo");
                evento1.setFechaInicio(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
                evento1.setFechaFin(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0));
                evento1.setUbicacion("Sala de conferencias");
                evento1.setUsuario(admin);
                calendarRepository.save(evento1);
            });
            
            // Buscar usuario y solicitud para el segundo evento
            usuarioRepository.findByUsername("usuario").ifPresent(usuario -> {
                solicitudRepository.findAll().stream()
                    .filter(s -> s.getTitulo().contains("materiales"))
                    .findFirst()
                    .ifPresent(solicitud -> {
                        // Agregar evento relacionado con la solicitud
                        Calendar evento2 = new Calendar();
                        evento2.setTitulo("Entrega de materiales");
                        evento2.setDescripcion("Entrega de materiales para el proyecto eco-amigable");
                        evento2.setFechaInicio(LocalDateTime.now().plusDays(3).withHour(14).withMinute(0));
                        evento2.setFechaFin(LocalDateTime.now().plusDays(3).withHour(15).withMinute(0));
                        evento2.setUbicacion("Almacén principal");
                        evento2.setUsuario(usuario);
                        evento2.setSolicitud(solicitud);
                        calendarRepository.save(evento2);
                    });
            });
        }
    }
    
    // Método para obtener todos los eventos
    public List<Calendar> findAll() {
        return calendarRepository.findAll();
    }
    
    // Método para buscar un evento por ID
    public Optional<Calendar> findById(Long id) {
        return calendarRepository.findById(id);
    }
    
    // Método para guardar un evento (crear o actualizar)
    public Calendar save(Calendar evento) {
        return calendarRepository.save(evento);
    }
    
    // Método para eliminar un evento
    public void deleteById(Long id) {
        calendarRepository.deleteById(id);
    }
    
    // Método para buscar eventos por usuario
    public List<Calendar> findByUsuario(Usuario usuario) {
        return calendarRepository.findByUsuario(usuario);
    }
    
    // Método para buscar eventos por solicitud
    public List<Calendar> findBySolicitud(Solicitud solicitud) {
        return calendarRepository.findBySolicitud(solicitud);
    }
}
