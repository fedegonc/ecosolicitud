package com.ecosolicitud.estadisticas;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.solicitud.SolicitudMetricas;
import com.ecosolicitud.solicitud.MetricasService;

// Indicadores del servicio, totales y por centro. La usabilidad se mide con
// el cuestionario de la investigación (Google Forms), no acá.
@Service
@Transactional(readOnly = true)
public class EstadisticasService {

    private final MetricasService metricas;
    private final OrganizacionService organizaciones;

    public EstadisticasService(MetricasService metricas, OrganizacionService organizaciones) {
        this.metricas = metricas;
        this.organizaciones = organizaciones;
    }

    public EstadisticasInfo resumen(Periodo periodo) {
        var desde = periodo.desde(Instant.now());
        var porCentro = metricas.porCentro(desde);
        List<EstadisticasInfo.Centro> centros = organizaciones.todas().stream()
                .map(o -> new EstadisticasInfo.Centro(o.nombre(), o.ciudad(),
                        porCentro.getOrDefault(o.id(), SolicitudMetricas.ninguna())))
                .toList();
        return new EstadisticasInfo(periodo, metricas.total(desde), centros);
    }
}
