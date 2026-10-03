package com.ecosolicitud.solicitud;

import java.time.Instant;
import java.util.List;

import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;

public record SolicitudSemilla(String ciudadanoId, Ciudad ciudad, String direccion,
        String referencia, List<Material> materiales, String organizacionId, String nota,
        Estado estado, Instant creadaEn, Instant finalizadaEn) {
}
