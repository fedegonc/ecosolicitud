package com.ecosolicitud.estadisticas;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecosolicitud.opinion.OpinionService;
import com.ecosolicitud.solicitud.SolicitudService;

// Reúne los indicadores del servicio: lo que miden las solicitudes
// más lo que opinan las personas que lo usan.
@Service
@Transactional(readOnly = true)
public class EstadisticasService {

    private final SolicitudService solicitudes;
    private final OpinionService opiniones;

    public EstadisticasService(SolicitudService solicitudes,
            OpinionService opiniones) {
        this.solicitudes = solicitudes;
        this.opiniones = opiniones;
    }

    public EstadisticasInfo resumen() {
        var m = solicitudes.metricas();
        return new EstadisticasInfo(m.recibidas(), m.aceptadas(), m.completadas(),
                m.medianaRespuesta(), opiniones.promedio(), opiniones.cantidad());
    }
}
