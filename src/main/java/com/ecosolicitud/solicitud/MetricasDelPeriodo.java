package com.ecosolicitud.solicitud;

import java.util.Map;

// Las tres vistas de un período, calculadas sobre una sola carga de filas.
public record MetricasDelPeriodo(SolicitudMetricas total, Map<String, SolicitudMetricas> porCentro,
        SolicitudSeries series) {
}
