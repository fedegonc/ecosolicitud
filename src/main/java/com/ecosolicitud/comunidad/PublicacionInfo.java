package com.ecosolicitud.comunidad;

import java.time.Instant;

import com.ecosolicitud.shared.Fechas;

public record PublicacionInfo(long id, String titulo, String resumen, String cuerpoHtml, String imagen,
        TipoPublicacion tipo, Instant publicadaEn) {

    public String fecha() {
        return Fechas.corta(publicadaEn);
    }
}
