package com.ecosolicitud.solicitud;

import java.util.List;

// total es el conteo real en base: solicitudes puede venir recortada por tope
public record GrupoSolicitudes(String clave, List<SolicitudInfo> solicitudes, long total) {
}
