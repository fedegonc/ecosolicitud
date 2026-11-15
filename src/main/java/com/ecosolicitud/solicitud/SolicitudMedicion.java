package com.ecosolicitud.solicitud;

import java.time.Instant;

public interface SolicitudMedicion {

    Estado getEstado();

    Instant getCreadaEn();

    Instant getRespondidaEn();

    Instant getAceptadaEn();
}
