package com.ecosolicitud.solicitud;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    private List<Solicitud> creadasDesde(Instant desde) {
        return repositorio.findAll().stream()
                .filter(s -> desde == null || !s.getCreadaEn().isBefore(desde)).toList();
    }
}
