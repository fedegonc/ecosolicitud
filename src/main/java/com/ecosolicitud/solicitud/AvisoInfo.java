package com.ecosolicitud.solicitud;

import java.time.Instant;

// Un aviso tal como se muestra en la bandeja.
public record AvisoInfo(long id, TipoAviso tipo, String detalle, Long solicitudId,
        boolean leida, Instant creadaEn) {
}
