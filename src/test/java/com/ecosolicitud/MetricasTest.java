package com.ecosolicitud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MetricasTest {

    @Test
    void un5xxApareceEnElResumen() {
        var m = new Metricas();
        m.registrar(200, 10);
        m.registrar(404, 5);
        m.registrar(500, 42);
        var r = m.resumen();
        assertEquals(1L, r.get("pedidos_2xx"));
        assertEquals(1L, r.get("pedidos_4xx"));
        assertEquals(1L, r.get("pedidos_5xx"));
        assertEquals("ok", r.get("estado"));
    }
}
