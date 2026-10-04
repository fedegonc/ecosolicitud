package com.ecosolicitud.solicitud;

import java.time.Duration;

// Recuento de solicitudes desde la mirada del servicio:
// aceptadas = pasaron por EN_CURSO alguna vez; mediana = recepción → primera respuesta.
public record SolicitudMetricas(long recibidas, long aceptadas, long completadas,
        Duration medianaRespuesta) {
}
