package com.ecosolicitud.solicitud;

import java.time.Instant;
import java.util.List;

import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Fechas;
import com.ecosolicitud.shared.Material;

public record SolicitudInfo(long id, Ciudad ciudad, String direccion, String referencia,
        List<Material> materiales, String organizacionNombre, String nombreCiudadano,
        String nota, Estado estado, Instant creadaEn, Instant finalizadaEn, long version) {

    public String creada() {
        return Fechas.corta(creadaEn);
    }

    public String finalizada() {
        return Fechas.corta(finalizadaEn);
    }
}
