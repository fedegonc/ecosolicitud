package com.ecosolicitud.solicitud;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Actor;
import com.ecosolicitud.solicitud.interno.Aviso;
import com.ecosolicitud.solicitud.interno.AvisoRepository;
import com.ecosolicitud.solicitud.interno.Solicitud;
import com.ecosolicitud.solicitud.interno.SolicitudRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toMap;

// Avisos que generan las solicitudes: al ciudadano cuando cambia el estado,
// a la organización cuando llega una nueva.
@Service
public class AvisoService {

    private static final int TAMANO_PAGINA = 12;

    private final AvisoRepository avisos;
    private final SolicitudRepository solicitudes;
    private final OrganizacionService organizaciones;

    public AvisoService(AvisoRepository avisos, SolicitudRepository solicitudes,
            OrganizacionService organizaciones) {
        this.avisos = avisos;
        this.solicitudes = solicitudes;
        this.organizaciones = organizaciones;
    }

    @Transactional
    public Page<AvisoInfo> avisosPara(Actor actor, int numero) {
        var destino = destino(actor);
        // JPA admite offsets de tipo int, aunque PageRequest calcula con long.
        int indice = Math.min(Math.max(1, numero) - 1, Integer.MAX_VALUE / TAMANO_PAGINA);
        var pagina = avisos.findByDestinoOrderByCreadaEnDescIdDesc(destino,
                PageRequest.of(indice, TAMANO_PAGINA));
        if (pagina.isEmpty() && pagina.getTotalElements() > 0) {
            pagina = avisos.findByDestinoOrderByCreadaEnDescIdDesc(destino,
                    PageRequest.of(pagina.getTotalPages() - 1, TAMANO_PAGINA));
        }
        var estados = estadosDe(pagina.getContent());
        // La vista conserva el resaltado de los nuevos; solo se leen los mostrados.
        var resultado = pagina.map(a -> aInfo(a, estados));
        var sinLeer = pagina.stream().filter(a -> !a.isLeida()).map(Aviso::getId).toList();
        if (!sinLeer.isEmpty()) {
            avisos.marcarLeidas(destino, sinLeer);
        }
        return resultado;
    }

    // kanban para la organización: cada aviso cae en la columna
    // donde está su solicitud ahora, no donde estaba al avisar.
    public List<ColumnaAvisos> kanbanPara(List<AvisoInfo> lista) {
        return List.of(
                new ColumnaAvisos("org.grupo.abiertas",
                        lista.stream().filter(a -> !a.estado().esFinal()).toList()),
                new ColumnaAvisos("org.grupo.cerradas",
                        lista.stream().filter(a -> a.estado().esFinal()).toList()));
    }

    public long avisosSinLeer(Actor actor) {
        return avisos.countByDestinoAndLeidaFalse(destino(actor));
    }

    @Transactional
    public void reemplazarAvisos(List<AvisoSemilla> dataset) {
        dataset.forEach(a -> {
            var aviso = new Aviso(a.destino(), a.solicitudId(), a.tipo(),
                    a.detalle(), a.creadaEn());
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

    private Map<Long, Estado> estadosDe(List<Aviso> lista) {
        var ids = lista.stream().map(Aviso::getSolicitudId)
                .filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return solicitudes.findAllById(ids).stream()
                .collect(toMap(Solicitud::getId, Solicitud::getEstado));
    }

    private String destino(Actor actor) {
        return actor.esOrganizacion() ? organizaciones.actual(actor).id() : actor.ciudadanoId();
    }

    private static AvisoInfo aInfo(Aviso a, Map<Long, Estado> estados) {
        var estado = a.getSolicitudId() != null
                ? estados.getOrDefault(a.getSolicitudId(), a.getTipo().getEstado())
                : a.getTipo().getEstado();
        return new AvisoInfo(a.getId(), a.getTipo(), a.getDetalle(),
                a.getSolicitudId(), a.isLeida(), a.getCreadaEn(), estado);
    }
}
