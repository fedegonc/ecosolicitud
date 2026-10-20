package com.ecosolicitud.solicitud;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    public SolicitudMetricas total(Instant desde) {
        return SolicitudMetricas.desde(creadasDesde(desde), Instant.now());
    }

    public Map<String, SolicitudMetricas> porCentro(Instant desde) {
        var ahora = Instant.now();
        return creadasDesde(desde).stream()
                .collect(groupingBy(Solicitud::getOrganizacionId,
                        collectingAndThen(toList(), l -> SolicitudMetricas.desde(l, ahora))));
    }

    // series de los gráficos; desde == null agrupa por mes todo el historial
    public SolicitudSeries series(Instant desde) {
        var hoy = LocalDate.now(Fechas.ZONA);
        return SolicitudSeries.desde(creadasDesde(desde),
                desde == null ? null : desde.atZone(Fechas.ZONA).toLocalDate(), hoy);
    }

    private List<Solicitud> creadasDesde(Instant desde) {
        return desde == null ? repositorio.findAll()
                : repositorio.findByCreadaEnGreaterThanEqual(desde);
    }
}
