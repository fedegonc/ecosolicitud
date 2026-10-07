package com.ecosolicitud.solicitud;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import com.ecosolicitud.shared.Fechas;
import com.ecosolicitud.solicitud.interno.Solicitud;

// Recuento de solicitudes desde la mirada del servicio:
// aceptadas = pasaron por EN_CURSO alguna vez; mediana = recepción → primera respuesta.
public record SolicitudMetricas(long recibidas, long aceptadas, long completadas,
        Duration medianaRespuesta) {

    static SolicitudMetricas desdeSemana(List<Solicitud> todas) {
        var inicio = LocalDate.now(Fechas.ZONA).with(DayOfWeek.MONDAY)
                .atStartOfDay(Fechas.ZONA).toInstant();
        return desde(todas.stream().filter(s -> s.getCreadaEn().isAfter(inicio)).toList());
    }

    public String mediana() {
        if (medianaRespuesta == null) {
            return "—";
        }
        long horas = medianaRespuesta.toHours();
        return horas < 48 ? horas + " h" : medianaRespuesta.toDays() + " d";
    }

    static SolicitudMetricas desde(List<Solicitud> todas) {
        long aceptadas = todas.stream().filter(s -> s.getEstado() == Estado.EN_CURSO
                || s.getEstado() == Estado.COMPLETADA).count();
        long completadas = todas.stream()
                .filter(s -> s.getEstado() == Estado.COMPLETADA).count();
        var respuestas = todas.stream().filter(s -> s.getRespondidaEn() != null)
                .map(s -> Duration.between(s.getCreadaEn(), s.getRespondidaEn()))
                .sorted().toList();
        return new SolicitudMetricas(todas.size(), aceptadas, completadas,
                mediana(respuestas));
    }

    private static Duration mediana(List<Duration> tiempos) {
        if (tiempos.isEmpty()) {
            return null;
        }
        int n = tiempos.size();
        if (n % 2 == 1) {
            return tiempos.get(n / 2);
        }
        return tiempos.get(n / 2 - 1).plus(tiempos.get(n / 2)).dividedBy(2);
    }
}
