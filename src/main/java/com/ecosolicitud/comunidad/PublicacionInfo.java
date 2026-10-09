package com.ecosolicitud.comunidad;

import java.time.Instant;

import com.ecosolicitud.shared.Fechas;

public record PublicacionInfo(long id, String titulo, String resumen, String cuerpoHtml,
        TipoPublicacion tipo, Instant publicadaEn) {

    public String fecha() {
        return Fechas.corta(publicadaEn);
    }
}
