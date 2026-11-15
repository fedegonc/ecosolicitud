package com.ecosolicitud.solicitud;

import java.time.OffsetDateTime;

public interface SolicitudTableroFila {

    Long getId();

    String getDireccion();

    String getReferencia();

    Double getLatitud();

    Double getLongitud();

    String getContacto();

    String getNota();

    String getEstado();

    OffsetDateTime getCreadaEn();

    OffsetDateTime getRespondidaEn();

    OffsetDateTime getAceptadaEn();

    OffsetDateTime getFinalizadaEn();

    Long getVersion();

    String getCiudadanoNombre();

    Long getMaterialId();

    String getMaterialCodigo();

    String getMaterialNombre();

    String getMaterialIcono();

    Boolean getMaterialActivo();

    String getOrganizacionNombre();

    Long getTotalPendiente();

    Long getTotalEnCurso();

    Long getTotalCompletada();

    Long getTotalRechazada();

    Long getTotalCancelada();
}
