package com.ecosolicitud.solicitud;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.ecosolicitud.shared.Fechas;
import com.ecosolicitud.solicitud.interno.Solicitud;

// Indicadores de un conjunto de solicitudes. Cada tasa divide solo por lo ya
// decidido, así una solicitud nueva no la mueve:
//   aceptación = aceptadas / respondidas
//   resolución = completadas / aceptadas que ya se cerraron
// Las pendientes no entran en la mediana: se informan aparte con su espera más larga.
public record SolicitudMetricas(long recibidas, long pendientes, long respondidas,
        long aceptadas, long completadas, long aceptadasCerradas,
        Duration medianaRespuesta, Duration esperaMasLarga) {

    public static SolicitudMetricas ninguna() {
        return new SolicitudMetricas(0, 0, 0, 0, 0, 0, null, null);
    }

    static SolicitudMetricas desdeSemana(List<Solicitud> todas, Instant ahora) {
        var inicio = LocalDate.ofInstant(ahora, Fechas.ZONA).with(DayOfWeek.MONDAY)
                .atStartOfDay(Fechas.ZONA).toInstant();
        return desde(todas.stream().filter(s -> !s.getCreadaEn().isBefore(inicio)).toList(), ahora);
    }

    static SolicitudMetricas desde(List<Solicitud> todas, Instant ahora) {
        long pendientes = 0, respondidas = 0, aceptadas = 0, completadas = 0, aceptadasCerradas = 0;
        Instant pendienteMasVieja = null;
        var respuestas = new ArrayList<Duration>();
        for (var s : todas) {
            if (s.getEstado() == Estado.PENDIENTE) {
                pendientes++;
                if (pendienteMasVieja == null || s.getCreadaEn().isBefore(pendienteMasVieja)) {
                    pendienteMasVieja = s.getCreadaEn();
                }
            }
            if (s.getRespondidaEn() != null) {
                respondidas++;
                respuestas.add(Duration.between(s.getCreadaEn(), s.getRespondidaEn()));
            }
            if (s.getAceptadaEn() != null) {
                aceptadas++;
                if (s.getEstado().esFinal()) {
                    aceptadasCerradas++;
                }
            }
            if (s.getEstado() == Estado.COMPLETADA) {
                completadas++;
            }
        }
        return new SolicitudMetricas(todas.size(), pendientes, respondidas, aceptadas,
                completadas, aceptadasCerradas, mediana(respuestas),
                pendienteMasVieja == null ? null : Duration.between(pendienteMasVieja, ahora));
    }

    public String aceptacion() {
        return porcentaje(aceptadas, respondidas);
    }

    public String resolucion() {
        return porcentaje(completadas, aceptadasCerradas);
    }

    public String mediana() {
        return duracion(medianaRespuesta);
    }

    public String espera() {
        return duracion(esperaMasLarga);
    }

    private static String porcentaje(long parte, long total) {
        return total == 0 ? "—" : String.format(Locale.ROOT, "%.0f%%", 100.0 * parte / total);
    }

    private static String duracion(Duration d) {
        if (d == null) {
            return "—";
        }
        long horas = d.toHours();
        return horas < 48 ? horas + " h" : d.toDays() + " d";
    }

    private static Duration mediana(List<Duration> tiempos) {
        if (tiempos.isEmpty()) {
            return null;
        }
        tiempos.sort(null);
        int n = tiempos.size();
        if (n % 2 == 1) {
            return tiempos.get(n / 2);
        }
        return tiempos.get(n / 2 - 1).plus(tiempos.get(n / 2)).dividedBy(2);
    }
}
