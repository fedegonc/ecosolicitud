package com.ecosolicitud.solicitud;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Actor;
import com.ecosolicitud.shared.Fechas;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.shared.Ubicacion;
import com.ecosolicitud.solicitud.TableroSolicitudes.OrganizacionTablero;
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
    private static final int TOPE_TABLERO = 200;
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

    public TableroSolicitudes tablero(Actor actor) {
        var filas = repositorio.tablero(actor.organizacionId(), TOPE_TABLERO);
        var solicitudes = aTablero(filas);
        var ahora = Instant.now();
        var desde = SolicitudMetricas.inicioDeSemana(ahora);
        var deLaSemana = solicitudes.stream()
                .filter(s -> !s.getCreadaEn().isBefore(desde)).toList();
        var nombre = filas.stream().findFirst().orElseThrow().getOrganizacionNombre();
        return new TableroSolicitudes(new OrganizacionTablero(nombre),
                bandeja(solicitudes, filas),
                SolicitudMetricas.desde(deLaSemana, ahora));
    }

    public BandejaSolicitudes recibidas(Actor actor) {
        return tablero(actor).bandeja();
    }

    public SolicitudMetricas semana(Actor actor) {
        var ahora = Instant.now();
        var desde = SolicitudMetricas.inicioDeSemana(ahora);
        if (actor.esOrganizacion()) {
            return tablero(actor).semana();
        }
        var deLaSemana = repositorio.findByCiudadano_IdAndCreadaEnGreaterThanEqualOrderByCreadaEnDesc(
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

    private String organizacionId(Actor actor) {
        return organizaciones.actual(actor).id();
    }

    private List<SolicitudTablero> aTablero(List<SolicitudTableroFila> filas) {
        var porId = new LinkedHashMap<Long, List<SolicitudTableroFila>>();
        filas.stream().filter(f -> f.getId() != null)
                .forEach(f -> porId.computeIfAbsent(f.getId(), id -> new ArrayList<>()).add(f));
        return porId.values().stream().map(this::aSolicitudTablero).toList();
    }

    private SolicitudTablero aSolicitudTablero(List<SolicitudTableroFila> filas) {
        var f = filas.get(0);
        var ubicacion = f.getLatitud() == null || f.getLongitud() == null ? null
                : new Ubicacion(f.getLatitud(), f.getLongitud());
        var materiales = filas.stream().filter(m -> m.getMaterialCodigo() != null)
                .map(m -> new Material(m.getMaterialId(), m.getMaterialCodigo(),
                        m.getMaterialNombre(), m.getMaterialIcono(),
                        Boolean.TRUE.equals(m.getMaterialActivo()))).toList();
        var info = new SolicitudInfo(f.getId(), f.getDireccion(), f.getReferencia(),
                ubicacion, materiales, f.getOrganizacionNombre(), f.getCiudadanoNombre(),
                f.getContacto(), f.getNota(), Estado.valueOf(f.getEstado()),
                instante(f.getCreadaEn()), instante(f.getFinalizadaEn()), f.getVersion());
        return new SolicitudTablero(info, instante(f.getRespondidaEn()),
                instante(f.getAceptadaEn()));
    }

    private static Instant instante(OffsetDateTime t) {
        return t == null ? null : t.toInstant();
    }

    private BandejaSolicitudes bandeja(List<SolicitudTablero> solicitudes,
            List<SolicitudTableroFila> filas) {
        return new BandejaSolicitudes(List.of(
                grupo(solicitudes, filas, Estado.PENDIENTE, TOPE_GRUPO),
                grupo(solicitudes, filas, Estado.EN_CURSO, TOPE_GRUPO),
                grupo(solicitudes, filas, Estado.COMPLETADA, TOPE_CERRADAS),
                grupo(solicitudes, filas, Estado.RECHAZADA, TOPE_CERRADAS),
                grupo(solicitudes, filas, Estado.CANCELADA, TOPE_CERRADAS)));
    }

    private GrupoSolicitudes grupo(List<SolicitudTablero> solicitudes,
            List<SolicitudTableroFila> filas, Estado estado, int tope) {
        var visibles = solicitudes.stream().filter(s -> s.getEstado() == estado)
                .limit(tope).map(SolicitudTablero::info).toList();
        return new GrupoSolicitudes(estado.getClave(), visibles, total(filas, estado));
    }

    private static long total(List<SolicitudTableroFila> filas, Estado estado) {
        if (filas.isEmpty()) {
            return 0;
        }
        var f = filas.get(0);
        return switch (estado) {
            case PENDIENTE -> f.getTotalPendiente();
            case EN_CURSO -> f.getTotalEnCurso();
            case COMPLETADA -> f.getTotalCompletada();
            case RECHAZADA -> f.getTotalRechazada();
            case CANCELADA -> f.getTotalCancelada();
        };
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
