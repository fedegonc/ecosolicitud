package com.ecosolicitud.solicitud;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Actor;
import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.solicitud.interno.Ciudadano;
import com.ecosolicitud.solicitud.interno.CiudadanoRepository;
import com.ecosolicitud.solicitud.interno.Solicitud;
import com.ecosolicitud.solicitud.interno.SolicitudRepository;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toSet;

@Service
public class SolicitudService {

    private final SolicitudRepository repositorio;
    private final CiudadanoRepository ciudadanos;
    private final AvisoService avisos;
    private final OrganizacionService organizaciones;

    public SolicitudService(SolicitudRepository repositorio,
            CiudadanoRepository ciudadanos, AvisoService avisos,
            OrganizacionService organizaciones) {
        this.repositorio = repositorio;
        this.ciudadanos = ciudadanos;
        this.avisos = avisos;
        this.organizaciones = organizaciones;
    }

    public List<SolicitudInfo> misSolicitudes(String ciudadanoId, Filtro filtro) {
        var propias = repositorio.findByCiudadano_IdOrderByCreadaEnDesc(ciudadanoId).stream()
                .filter(s -> filtro.muestra(s.getEstado())).toList();
        return aInfos(propias);
    }

    public BandejaSolicitudes recibidas(Actor actor) {
        String organizacionId = organizaciones.actual(actor).id();
        var lista = aInfos(repositorio.findByOrganizacionIdOrderByCreadaEnDesc(organizacionId));
        return new BandejaSolicitudes(List.of(
                new GrupoSolicitudes("org.grupo.pendientes",
                        lista.stream().filter(s -> s.estado() == Estado.PENDIENTE).toList()),
                new GrupoSolicitudes("org.grupo.en-curso",
                        lista.stream().filter(s -> s.estado() == Estado.EN_CURSO).toList()),
                new GrupoSolicitudes("org.grupo.cerradas",
                        lista.stream().filter(s -> s.estado().esFinal()).toList())));
    }

    @Transactional
    public Optional<SolicitudInfo> crear(Actor actor, Ciudad ciudad, String direccion,
            String referencia, List<Material> materiales, String organizacionId,
            String nota) {
        boolean compatible = organizaciones.compatibles(ciudad, materiales).stream()
                .anyMatch(o -> o.id().equals(organizacionId));
        if (!compatible) {
            return Optional.empty();
        }
        var s = Solicitud.nueva(new SolicitudSemilla(actor.ciudadanoId(),
                actor.nombreCiudadano(), direccion, referencia, materiales,
                organizacionId, nota, Estado.PENDIENTE, Instant.now(), null, null),
                ciudadano(actor.ciudadanoId(), actor.nombreCiudadano()));
        repositorio.save(s);
        avisos.nueva(s);
        String nombre = organizaciones.buscar(organizacionId)
                .map(OrganizacionInfo::nombre).orElse(organizacionId);
        return Optional.of(aInfo(s, Map.of(organizacionId, nombre)));
    }

    @Transactional
    public Resultado aceptar(Actor actor, long id, long version) {
        return aplicar(actor, id, version, s -> esDestinataria(actor, s),
                s -> s.aceptar(Instant.now()), TipoAviso.ACEPTADA);
    }

    @Transactional
    public Resultado rechazar(Actor actor, long id, long version) {
        return aplicar(actor, id, version, s -> esDestinataria(actor, s),
                s -> s.rechazar(Instant.now()), TipoAviso.RECHAZADA);
    }

    @Transactional
    public Resultado completar(Actor actor, long id, long version) {
        return aplicar(actor, id, version, s -> esDestinataria(actor, s),
                s -> s.completar(Instant.now()), TipoAviso.COMPLETADA);
    }

    @Transactional
    public Resultado cancelar(Actor actor, long id, long version) {
        return aplicar(actor, id, version, s -> esAutor(actor, s),
                s -> s.cancelar(Instant.now()), TipoAviso.CANCELADA);
    }

    public List<SolicitudInfo> todas() {
        var todas = repositorio.findAll(Sort.by("id"));
        var nombres = organizaciones.nombres(todas.stream()
                .map(Solicitud::getOrganizacionId).collect(toSet()));
        return todas.stream().map(s -> aInfo(s, nombres)).toList();
    }

    public boolean vacia() {
        return repositorio.count() == 0;
    }

    public SolicitudMetricas metricas() {
        return SolicitudMetricas.desde(repositorio.findAll());
    }

    @Transactional
    public void reemplazarTodas(List<SolicitudSemilla> dataset) {
        avisos.borrarTodas();
        repositorio.deleteAll();
        ciudadanos.deleteAll();
        dataset.forEach(s -> repositorio.save(
                Solicitud.nueva(s, ciudadano(s.ciudadanoId(), s.nombreCiudadano()))));
    }

    private Ciudadano ciudadano(String id, String nombre) {
        return ciudadanos.findById(id).orElseGet(() -> {
            var nuevo = new Ciudadano();
            nuevo.setId(id);
            nuevo.setNombre(nombre);
            return ciudadanos.save(nuevo);
        });
    }

    private Resultado aplicar(Actor actor, long id, long version, Predicate<Solicitud> permiso,
            Consumer<Solicitud> transicion, TipoAviso tipoAviso) {
        var s = repositorio.findById(id).orElse(null);
        if (s == null) {
            return Resultado.NO_ENCONTRADA;
        }
        if (!permiso.test(s)) {
            return Resultado.SIN_PERMISO;
        }
        if (s.getVersion() != version) {
            return Resultado.CONFLICTO;
        }
        try {
            transicion.accept(s);
            repositorio.saveAndFlush(s);
            avisos.avisar(s, tipoAviso);
            return Resultado.OK;
        } catch (TransicionInvalidaException e) {
            return Resultado.INVALIDA;
        } catch (OptimisticLockingFailureException e) {
            return Resultado.CONFLICTO;
        }
    }

    private boolean esDestinataria(Actor actor, Solicitud s) {
        return actor.esOrganizacion()
                && s.getOrganizacionId().equals(actor.organizacionId());
    }

    private boolean esAutor(Actor actor, Solicitud s) {
        return actor.esCiudadano()
                && s.getCiudadano().getId().equals(actor.ciudadanoId());
    }

    // una sola consulta de nombres para toda la lista
    private List<SolicitudInfo> aInfos(List<Solicitud> solicitudes) {
        var nombres = organizaciones.nombres(solicitudes.stream()
                .map(Solicitud::getOrganizacionId).collect(toSet()));
        return solicitudes.stream().map(s -> aInfo(s, nombres)).toList();
    }

    private static SolicitudInfo aInfo(Solicitud s, Map<String, String> nombres) {
        return new SolicitudInfo(s.getId(), s.getDireccion(),
                s.getReferencia(), List.copyOf(s.getMateriales()),
                nombres.getOrDefault(s.getOrganizacionId(), s.getOrganizacionId()),
                s.getCiudadano().getNombre(), s.getNota(), s.getEstado(), s.getCreadaEn(),
                s.getFinalizadaEn(), s.getVersion());
    }
}
