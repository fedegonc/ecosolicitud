package com.ecosolicitud.solicitud;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.ecosolicitud.shared.Fechas;
import com.ecosolicitud.solicitud.interno.Solicitud;
import com.ecosolicitud.solicitud.interno.SolicitudRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

// Indicadores del servicio para un período; desde == null es todo el historial.
@Service
@Transactional(readOnly = true)
public class MetricasService {

    private final SolicitudRepository repositorio;

    public MetricasService(SolicitudRepository repositorio) {
        this.repositorio = repositorio;
    }

    // total, por centro y series sobre una sola carga (las estadísticas
    // necesitan las tres; pedirlas por separado cargaba las filas tres veces)
    public MetricasDelPeriodo delPeriodo(Instant desde) {
        var ahora = Instant.now();
        var creadas = creadasDesde(desde);
        var porCentro = creadas.stream()
                .collect(groupingBy(Solicitud::getOrganizacionId,
                        collectingAndThen(toList(), l -> SolicitudMetricas.desde(l, ahora))));
        var series = SolicitudSeries.desde(creadas,
                desde == null ? null : desde.atZone(Fechas.ZONA).toLocalDate(),
                LocalDate.now(Fechas.ZONA));
        return new MetricasDelPeriodo(SolicitudMetricas.desde(creadas, ahora), porCentro, series);
    }

    private List<Solicitud> creadasDesde(Instant desde) {
        return desde == null ? repositorio.findAll()
                : repositorio.findByCreadaEnGreaterThanEqual(desde);
    }
}
