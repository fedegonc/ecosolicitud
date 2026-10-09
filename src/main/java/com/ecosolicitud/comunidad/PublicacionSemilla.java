package com.ecosolicitud.comunidad;

import java.time.Instant;

// Carga del dataset: el contenido se publica cuando exista la autoría.
public record PublicacionSemilla(String titulo, String resumen, String cuerpo, String imagen,
        TipoPublicacion tipo, Instant publicadaEn) {
}
