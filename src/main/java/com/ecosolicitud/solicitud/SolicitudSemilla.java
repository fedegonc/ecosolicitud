package com.ecosolicitud.solicitud;

import java.time.Instant;
import java.util.List;

import com.ecosolicitud.shared.Material;

public record SolicitudSemilla(String ciudadanoId, String nombreCiudadano,
        String contacto, String direccion, String referencia, List<Material> materiales,
        String organizacionId, String nota, Estado estado, Instant creadaEn,
        Instant respondidaEn, Instant finalizadaEn) {
}
