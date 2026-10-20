package com.ecosolicitud.solicitud.interno;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.shared.Fechas;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.solicitud.SolicitudInfo;

// Copia de respaldo en texto plano: una fila por solicitud, columnas separadas
// por tabulador (se abre tal cual en una planilla). Los datos que cargó la
// gente no traen tabuladores ni saltos de línea: se vuelven espacios.
final class CopiaTexto {

    private static final String CABECERA = String.join("\t", "id", "creada", "finalizada",
            "estado", "ciudad", "nombre", "contacto", "materiales", "direccion",
            "referencia", "nota");

    private CopiaTexto() {
    }

    static String de(OrganizacionInfo org, List<SolicitudInfo> solicitudes) {
        return solicitudes.stream()
                .map(s -> String.join("\t", String.valueOf(s.id()), fecha(s.creadaEn()),
                        fecha(s.finalizadaEn()), s.estado().name(), org.ciudad().name(),
                        limpio(s.nombreCiudadano()), limpio(s.contacto()),
                        s.materiales().stream().map(Material::getNombre)
                                .collect(Collectors.joining(", ")),
                        limpio(s.direccion()), limpio(s.referencia()), limpio(s.nota())))
                .collect(Collectors.joining("\n", CABECERA + "\n", "\n"));
    }

    private static String fecha(Instant instante) {
        return instante == null ? ""
                : instante.atZone(Fechas.ZONA).toLocalDateTime().truncatedTo(ChronoUnit.MINUTES).toString();
    }

    private static String limpio(String texto) {
        return texto == null ? "" : texto.replaceAll("[\\t\\r\\n]+", " ").strip();
    }
}
