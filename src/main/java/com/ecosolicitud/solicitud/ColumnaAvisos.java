package com.ecosolicitud.solicitud;

import java.util.List;

// Una columna del kanban de avisos de la organización.
public record ColumnaAvisos(String clave, List<AvisoInfo> avisos) {
}
