package com.ecosolicitud.solicitud;

import java.time.Instant;

// Un aviso precargado del dataset de demostración.
public record AvisoSemilla(String destino, Long solicitudId, TipoAviso tipo,
        String detalle, boolean leida, Instant creadaEn) {
}
