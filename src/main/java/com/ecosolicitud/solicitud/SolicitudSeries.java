package com.ecosolicitud.solicitud;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.ecosolicitud.shared.Fechas;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.solicitud.interno.Solicitud;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

// Las series de los gráficos de estadísticas: cuántas solicitudes por estado,
// por material y en el tiempo. Cada barra trae su alto relativo al máximo de
// la serie (0–100) para que la vista solo lo pinte.
public record SolicitudSeries(List<Barra> porEstado, List<Barra> porMaterial,
        List<Barra> enElTiempo) {

    // clave: mensaje i18n (estado); etiqueta: texto literal (material, fecha)
    public record Barra(String clave, String etiqueta, long valor, int proporcion) {
    }

    private record Fila(String clave, String etiqueta, long valor) {
    }

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("d/M");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("M/yy");

    // desde == null: todo el historial, agrupado por mes; si no, por día
    static SolicitudSeries desde(List<Solicitud> solicitudes, LocalDate desde, LocalDate hasta) {
        var estados = solicitudes.stream().collect(groupingBy(Solicitud::getEstado, counting()));
        var materiales = solicitudes.stream().flatMap(s -> s.getMateriales().stream())
                .collect(groupingBy(Material::getNombre, counting()));
        return new SolicitudSeries(
                barras(Arrays.stream(Estado.values())
                        .map(e -> new Fila(e.getClave(), null, estados.getOrDefault(e, 0L))).toList()),
                barras(materiales.entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                        .map(e -> new Fila(null, e.getKey(), e.getValue())).toList()),
                barras(tiempo(solicitudes, desde, hasta)));
    }

    private static List<Fila> tiempo(List<Solicitud> solicitudes, LocalDate desde, LocalDate hasta) {
        Function<Solicitud, LocalDate> dia = s -> s.getCreadaEn().atZone(Fechas.ZONA).toLocalDate();
        var cuentas = new LinkedHashMap<String, Long>();
        if (desde == null) {
            var primero = solicitudes.stream().map(dia).min(Comparator.naturalOrder()).orElse(hasta);
            for (var m = YearMonth.from(primero); !m.isAfter(YearMonth.from(hasta)); m = m.plusMonths(1)) {
                cuentas.put(MES.format(m), 0L);
            }
            solicitudes.forEach(s -> cuentas.merge(MES.format(YearMonth.from(dia.apply(s))), 1L, Long::sum));
        } else {
            for (var d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
                cuentas.put(DIA.format(d), 0L);
            }
            solicitudes.forEach(s -> cuentas.computeIfPresent(DIA.format(dia.apply(s)), (k, v) -> v + 1));
        }
        return cuentas.entrySet().stream().map(e -> new Fila(null, e.getKey(), e.getValue())).toList();
    }

    private static List<Barra> barras(List<Fila> filas) {
        long max = filas.stream().mapToLong(Fila::valor).max().orElse(0);
        return filas.stream().map(f -> new Barra(f.clave(), f.etiqueta(), f.valor(),
                max == 0 ? 0 : (int) Math.round(100.0 * f.valor() / max))).toList();
    }
}
