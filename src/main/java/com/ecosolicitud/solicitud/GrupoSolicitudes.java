package com.ecosolicitud.solicitud;

import java.util.List;

public record GrupoSolicitudes(String clave, List<SolicitudInfo> solicitudes) {
}
