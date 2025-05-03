package com.ecosolicitud.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entidad Solicitud - Representa una solicitud en el sistema
 */
@Entity
@Table(name = "solicitudes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Solicitud {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;
    private String descripcion;
    private String estado; // "PENDIENTE", "APROBADA", "RECHAZADA", etc.
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    
    // Relación con Usuario (solicitante)
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
