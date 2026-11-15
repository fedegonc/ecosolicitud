package com.ecosolicitud.solicitud;

import java.time.Instant;
import java.util.List;

import com.ecosolicitud.shared.Material;
import com.ecosolicitud.shared.Ubicacion;

public record SolicitudSemilla(String ciudadanoId, String nombreCiudadano,
        String contacto, String direccion, String referencia, Ubicacion ubicacion,
        List<Material> materiales, String organizacionId, String nota, Estado estado,
        Instant creadaEn, Instant respondidaEn, Instant finalizadaEn) {

    // Para datos sin aceptadaEn: una en curso/completada se aceptó al responder;
    // una rechazada se aceptó antes solo si la respuesta precede al cierre.
    public Instant aceptadaEn() {
        if (respondidaEn == null) {
            return null;
        }
        return switch (estado) {
            case EN_CURSO, COMPLETADA -> respondidaEn;
            case RECHAZADA -> respondidaEn.isBefore(finalizadaEn) ? respondidaEn : null;
            default -> null;
        };
    }
}
