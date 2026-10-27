package com.ecosolicitud.estadisticas;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.CacheConVencimiento;
import com.ecosolicitud.shared.Fechas;
import com.ecosolicitud.solicitud.SolicitudMetricas;
import com.ecosolicitud.solicitud.MetricasService;

// Indicadores del servicio, totales y por centro. La usabilidad se mide con
// el cuestionario de la investigación (Google Forms), no acá.
// Se recalculan una vez por día por período: la página es pública y el
// cálculo recorre las solicitudes del período; un día de atraso no cambia
// lo que muestra en esta etapa ("actualizado" dice cuándo se calculó).
@Service
@Transactional(readOnly = true)
public class EstadisticasService {

    private final MetricasService metricas;
    private final OrganizacionService organizaciones;
    private final CacheConVencimiento<Periodo, EstadisticasInfo> diarias =
            new CacheConVencimiento<>(Duration.ofDays(1), Clock.systemUTC());

    public EstadisticasService(MetricasService metricas, OrganizacionService organizaciones) {
        this.metricas = metricas;
        this.organizaciones = organizaciones;
    }

    public EstadisticasInfo resumen(Periodo periodo) {
        return diarias.obtener(periodo, () -> calcular(periodo));
    }

    // al reemplazar el dataset (reinicio de la demo) las cifras guardadas ya no valen
    public void olvidar() {
        diarias.vaciar();
    }

    private EstadisticasInfo calcular(Periodo periodo) {
        var desde = periodo.desde(Instant.now());
        var delPeriodo = metricas.delPeriodo(desde);
        List<EstadisticasInfo.Centro> centros = organizaciones.todas().stream()
                .map(o -> new EstadisticasInfo.Centro(o.nombre(), o.ciudad(),
                        delPeriodo.porCentro().getOrDefault(o.id(), SolicitudMetricas.ninguna())))
                .toList();
        return new EstadisticasInfo(periodo, Fechas.corta(Instant.now()), delPeriodo.total(),
                delPeriodo.series(), centros);
    }
}
