package com.ecosolicitud.shared;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class Fechas {

    public static final ZoneId ZONA = ZoneId.of("America/Montevideo");
    private static final DateTimeFormatter CORTA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Fechas() {
    }

    public static String corta(Instant instante) {
        return instante == null ? "" : CORTA.format(instante.atZone(ZONA));
    }
}
