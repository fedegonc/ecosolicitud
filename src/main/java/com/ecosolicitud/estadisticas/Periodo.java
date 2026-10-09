package com.ecosolicitud.estadisticas;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

public enum Periodo {
    MES("30d", Duration.ofDays(30)),
    TODO("todo", null);

    private final String parametro;
    private final Duration ventana;

    Periodo(String parametro, Duration ventana) {
        this.parametro = parametro;
        this.ventana = ventana;
    }

    public String getParametro() {
        return parametro;
    }

    // null: sin límite inferior
    Instant desde(Instant ahora) {
        return ventana == null ? null : ahora.minus(ventana);
    }

    // un valor desconocido no es error: se muestra el período por defecto
    public static Periodo desdeParametro(String valor) {
        return Arrays.stream(values()).filter(p -> p.parametro.equals(valor)).findFirst().orElse(MES);
    }
}
