package com.ecosolicitud.estadisticas;

import java.time.Duration;
import java.util.Locale;

// Los indicadores que el sistema puede medir, ya listos para mostrar.
public record EstadisticasInfo(long recibidas, long aceptadas, long completadas,
        Duration medianaRespuesta, Double promedioOpinion, long cantidadOpiniones) {

    public String aceptacion() {
        return recibidas == 0 ? "—" : porcentaje(aceptadas);
    }

    public String resolucion() {
        return recibidas == 0 ? "—" : porcentaje(completadas);
    }

    public String mediana() {
        if (medianaRespuesta == null) {
            return "—";
        }
        long horas = medianaRespuesta.toHours();
        return horas < 48 ? horas + " h" : medianaRespuesta.toDays() + " d";
    }

    public String friccion() {
        return promedioOpinion == null ? "—"
                : String.format(Locale.ROOT, "%.1f", promedioOpinion);
    }

    private String porcentaje(long parte) {
        return String.format(Locale.ROOT, "%.0f%%", 100.0 * parte / recibidas);
    }
}
