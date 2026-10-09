package com.ecosolicitud.estadisticas;

import java.util.List;

import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.solicitud.SolicitudMetricas;

public record EstadisticasInfo(Periodo periodo, SolicitudMetricas total, List<Centro> centros) {

    public record Centro(String nombre, Ciudad ciudad, SolicitudMetricas metricas) {
    }
}
