package com.ecosolicitud.solicitud;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Actor;
import com.ecosolicitud.shared.Fechas;
import com.ecosolicitud.solicitud.interno.Solicitud;
import com.ecosolicitud.solicitud.interno.SolicitudRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Listados del panel: paginan en la base (LIMIT/OFFSET), no traen el historial
// completo a memoria. Las bandejas de la org usan topes por columna con su
// total real, y las métricas consultan solo el período que muestran.
@Service
@Transactional(readOnly = true)
public class ListadoService {

    private static final int TAMANO_PAGINA = 12;
    private static final int TOPE_GRUPO = 20;
    private static final int TOPE_CERRADAS = 5;

    private final SolicitudRepository repositorio;
    private final SolicitudService servicio;
    private final OrganizacionService organizaciones;

    public ListadoService(SolicitudRepository repositorio, SolicitudService servicio,
            OrganizacionService organizaciones) {
        this.repositorio = repositorio;
        this.servicio = servicio;
        this.organizaciones = organizaciones;
    }

    // la portada muestra solo la última: una consulta, sin count ni página
    public Optional<SolicitudInfo> ultimaDelCiudadano(String ciudadanoId) {
        return repositorio.findTop1ByCiudadano_IdOrderByCreadaEnDesc(ciudadanoId)
                .map(s -> servicio.aInfos(List.of(s)).get(0));
    }

    public Page<SolicitudInfo> misSolicitudes(String ciudadanoId, Filtro filtro,
            int pagina) {
        return pagina(pagina, p -> repositorio
                .findByCiudadano_IdAndEstadoInOrderByCreadaEnDesc(
                        ciudadanoId, filtro.estados(), p));
    }

    public BandejaSolicitudes recibidas(Actor actor) {
        var org = organizacionId(actor);
        return new BandejaSolicitudes(List.of(
                grupo(org, "org.grupo.pendientes", Set.of(Estado.PENDIENTE), TOPE_GRUPO),
                grupo(org, "org.grupo.en-curso", Set.of(Estado.EN_CURSO), TOPE_GRUPO),
                grupo(org, "org.grupo.cerradas", Estado.finales(), TOPE_CERRADAS)));
    }

    public Page<SolicitudInfo> archivadas(Actor actor, int pagina) {
        return pagina(pagina, p -> actor.esOrganizacion()
                ? repositorio.findByOrganizacionIdAndEstadoInOrderByCreadaEnDesc(
                        organizacionId(actor), Estado.finales(), p)
                : repositorio.findByCiudadano_IdAndEstadoInOrderByCreadaEnDesc(
                        actor.ciudadanoId(), Estado.finales(), p));
    }

    public long totalArchivadas(Actor actor) {
        return actor.esOrganizacion()
                ? repositorio.countByOrganizacionIdAndEstadoIn(
                        organizacionId(actor), Estado.finales())
                : repositorio.countByCiudadano_IdAndEstadoIn(
                        actor.ciudadanoId(), Estado.finales());
    }

    public SolicitudMetricas semana(Actor actor) {
        var ahora = Instant.now();
        var desde = SolicitudMetricas.inicioDeSemana(ahora);
        var deLaSemana = actor.esOrganizacion()
                ? repositorio.findByOrganizacionIdAndCreadaEnGreaterThanEqualOrderByCreadaEnDesc(
                        organizacionId(actor), desde)
                : repositorio.findByCiudadano_IdAndCreadaEnGreaterThanEqualOrderByCreadaEnDesc(
                        actor.ciudadanoId(), desde);
        return SolicitudMetricas.desde(deLaSemana, ahora);
    }

    public InformeMensual informe(Actor actor, YearMonth mes) {
        var desde = mes.atDay(1).atStartOfDay(Fechas.ZONA).toInstant();
        var hasta = mes.plusMonths(1).atDay(1).atStartOfDay(Fechas.ZONA).toInstant();
        var delMes = actor.esOrganizacion()
                ? repositorio.findByOrganizacionIdAndCreadaEnGreaterThanEqualAndCreadaEnLessThanOrderByCreadaEnDesc(
                        organizacionId(actor), desde, hasta)
                : repositorio.findByCiudadano_IdAndCreadaEnGreaterThanEqualAndCreadaEnLessThanOrderByCreadaEnDesc(
                        actor.ciudadanoId(), desde, hasta);
        return InformeMensual.desde(mes, delMes, servicio.aInfos(delMes));
    }

    // copia de respaldo: todas las solicitudes de una organización, sin tope
    public List<SolicitudInfo> todasDe(String organizacionId) {
        return servicio.aInfos(repositorio.findByOrganizacionIdOrderByCreadaEnDesc(organizacionId));
    }

    private String organizacionId(Actor actor) {
        return organizaciones.actual(actor).id();
    }

    private GrupoSolicitudes grupo(String organizacionId, String clave,
            Set<Estado> estados, int tope) {
        var p = repositorio.findByOrganizacionIdAndEstadoInOrderByCreadaEnDesc(
                organizacionId, estados, PageRequest.of(0, tope));
        return new GrupoSolicitudes(clave, servicio.aInfos(p.getContent()),
                p.getTotalElements());
    }

    // pagina es el número visible (1 en adelante); fuera de rango cae en la última
    private Page<SolicitudInfo> pagina(int numero,
            Function<Pageable, Page<Solicitud>> consulta) {
        var p = consulta.apply(PageRequest.of(Math.max(0, numero - 1), TAMANO_PAGINA));
        if (p.isEmpty() && p.getTotalElements() > 0) {
            p = consulta.apply(PageRequest.of(p.getTotalPages() - 1, TAMANO_PAGINA));
        }
        return new PageImpl<>(servicio.aInfos(p.getContent()), p.getPageable(),
                p.getTotalElements());
    }
}
