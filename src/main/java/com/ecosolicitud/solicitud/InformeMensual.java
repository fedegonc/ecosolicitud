package com.ecosolicitud.solicitud;

import java.time.YearMonth;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ecosolicitud.shared.Fechas;
import com.ecosolicitud.solicitud.interno.Solicitud;

public record InformeMensual(YearMonth mes, SolicitudMetricas metricas,
        Map<Estado, Long> porEstado, Map<String, Long> porMaterial,
        List<SolicitudInfo> solicitudes) {

    public static InformeMensual desde(YearMonth mes, List<Solicitud> entidades,
            List<SolicitudInfo> infos) {
        var porEstado = new EnumMap<Estado, Long>(Estado.class);
        var porMaterial = new LinkedHashMap<String, Long>();
        for (var s : entidades) {
            porEstado.merge(s.getEstado(), 1L, Long::sum);
            s.getMateriales().forEach(m -> porMaterial.merge(m.getNombre(), 1L, Long::sum));
        }
        return new InformeMensual(mes, SolicitudMetricas.desde(entidades),
                porEstado, porMaterial, infos);
    }

    static boolean esDelMes(Solicitud s, YearMonth mes) {
        return mes.equals(YearMonth.from(s.getCreadaEn().atZone(Fechas.ZONA)));
    }
}
