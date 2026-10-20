package com.ecosolicitud.estadisticas;

import java.util.List;

import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.solicitud.SolicitudMetricas;
import com.ecosolicitud.solicitud.SolicitudSeries;

// actualizado: hora de la consulta ya formateada, para la cabecera del panel
public record EstadisticasInfo(Periodo periodo, String actualizado, SolicitudMetricas total,
        SolicitudSeries series, List<Centro> centros) {

    public record Centro(String nombre, Ciudad ciudad, SolicitudMetricas metricas) {
    }
}
