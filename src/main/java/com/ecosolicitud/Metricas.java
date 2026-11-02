package com.ecosolicitud;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

// Contadores mínimos de monitoreo, llenados por TiemposFiltro:
// pedidos por familia de estado y una ventana de duraciones para el p95.
// Sin rutas, usuarios ni datos personales: solo números agregados.
@Component
final class Metricas {

    private static final int VENTANA = 512;

    private final AtomicLong ok = new AtomicLong();
    private final AtomicLong err4 = new AtomicLong();
    private final AtomicLong err5 = new AtomicLong();
    private final long inicio = System.currentTimeMillis();
    private final long[] duraciones = new long[VENTANA];
    private final AtomicInteger siguiente = new AtomicInteger();

    void registrar(int estado, long ms) {
        if (estado >= 500) {
            err5.incrementAndGet();
        } else if (estado >= 400) {
            err4.incrementAndGet();
        } else {
            ok.incrementAndGet();
        }
        duraciones[Math.floorMod(siguiente.getAndIncrement(), VENTANA)] = ms;
    }

    Map<String, Object> resumen() {
        long[] ventana = duraciones.clone();
        int llenas = (int) Math.min(siguiente.get(), VENTANA);
        Arrays.sort(ventana, 0, llenas);
        var res = new LinkedHashMap<String, Object>();
        res.put("estado", "ok");
        res.put("segundos_arriba", (System.currentTimeMillis() - inicio) / 1000);
        res.put("pedidos_2xx", ok.get());
        res.put("pedidos_4xx", err4.get());
        res.put("pedidos_5xx", err5.get());
        res.put("p95_ms_ultimos", llenas == 0 ? 0 : ventana[(int) Math.ceil(llenas * 0.95) - 1]);
        var rt = Runtime.getRuntime();
        res.put("memoria_usada_mb", (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024));
        return res;
    }
}
