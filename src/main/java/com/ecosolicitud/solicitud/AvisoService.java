package com.ecosolicitud.solicitud;

import java.time.Instant;
import java.util.List;

import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Actor;
import com.ecosolicitud.solicitud.interno.Aviso;
import com.ecosolicitud.solicitud.interno.AvisoRepository;
import com.ecosolicitud.solicitud.interno.Solicitud;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Avisos que generan las solicitudes: al ciudadano cuando cambia el estado,
// a la organización cuando llega una nueva.
@Service
public class AvisoService {

    private final AvisoRepository avisos;
    private final OrganizacionService organizaciones;

    public AvisoService(AvisoRepository avisos, OrganizacionService organizaciones) {
        this.avisos = avisos;
        this.organizaciones = organizaciones;
    }

    public List<AvisoInfo> avisosPara(Actor actor) {
        return avisos.findByDestinoOrderByCreadaEnDesc(destino(actor)).stream()
                .map(AvisoService::aInfo).toList();
    }

    public long avisosSinLeer(Actor actor) {
        return avisos.countByDestinoAndLeidaFalse(destino(actor));
    }

    @Transactional
    public void marcarLeidas(Actor actor) {
        avisos.findByDestinoAndLeidaFalse(destino(actor))
                .forEach(Aviso::marcarLeida);
    }

    @Transactional
    public void reemplazarAvisos(List<AvisoSemilla> dataset) {
        dataset.forEach(a -> {
            var aviso = new Aviso(a.destino(), null, a.tipo(), a.detalle(), a.creadaEn());
            if (a.leida()) {
                aviso.marcarLeida();
            }
            avisos.save(aviso);
        });
    }

    void nueva(Solicitud s) {
        avisos.save(new Aviso(s.getOrganizacionId(), s.getId(), TipoAviso.NUEVA,
                s.getCiudadano().getNombre(), Instant.now()));
    }

    void avisar(Solicitud s, TipoAviso tipo) {
        String destino = tipo.esParaCiudadano()
                ? s.getCiudadano().getId() : s.getOrganizacionId();
        String detalle = tipo.esParaCiudadano()
                ? organizaciones.buscar(s.getOrganizacionId())
                        .map(OrganizacionInfo::nombre).orElse(s.getOrganizacionId())
                : s.getCiudadano().getNombre();
        avisos.save(new Aviso(destino, s.getId(), tipo, detalle, Instant.now()));
    }

    void borrarTodas() {
        avisos.deleteAll();
    }

    private static String destino(Actor actor) {
        return actor.esOrganizacion() ? actor.organizacionId() : actor.ciudadanoId();
    }

    private static AvisoInfo aInfo(Aviso a) {
        return new AvisoInfo(a.getId(), a.getTipo(), a.getDetalle(),
                a.getSolicitudId(), a.isLeida(), a.getCreadaEn());
    }
}
