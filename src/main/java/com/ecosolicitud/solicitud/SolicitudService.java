package com.ecosolicitud.solicitud;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.solicitud.interno.Solicitud;
import com.ecosolicitud.solicitud.interno.SolicitudRepository;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
public class SolicitudService {

    private final SolicitudRepository repositorio;
    private final OrganizacionService organizaciones;
    private final ActorSesion actor;

    public SolicitudService(SolicitudRepository repositorio,
            OrganizacionService organizaciones, ActorSesion actor) {
        this.repositorio = repositorio;
        this.organizaciones = organizaciones;
        this.actor = actor;
    }

    public List<SolicitudInfo> misSolicitudes(String ciudadanoId, Filtro filtro) {
        var propias = repositorio.findByCiudadanoIdOrderByCreadaEnDesc(ciudadanoId).stream()
                .filter(s -> filtro.muestra(s.getEstado())).toList();
        var nombres = organizaciones.nombres(propias.stream()
                .map(Solicitud::getOrganizacionId).collect(toSet()));
        return propias.stream().map(s -> aInfo(s, nombres)).toList();
    }

    public BandejaSolicitudes recibidas(String organizacionId) {
        var lista = repositorio.findByOrganizacionIdOrderByCreadaEnDesc(organizacionId)
                .stream().map(s -> aInfo(s, organizaciones.nombres(Set.of(organizacionId))))
                .toList();
        return new BandejaSolicitudes(List.of(
                new GrupoSolicitudes("org.grupo.pendientes",
                        lista.stream().filter(s -> s.estado() == Estado.PENDIENTE).toList()),
                new GrupoSolicitudes("org.grupo.en-curso",
                        lista.stream().filter(s -> s.estado() == Estado.EN_CURSO).toList()),
                new GrupoSolicitudes("org.grupo.cerradas",
                        lista.stream().filter(s -> s.estado().esFinal()).toList())));
    }

    @Transactional
    public Optional<SolicitudInfo> crear(Ciudad ciudad, String direccion,
            String referencia, List<Material> materiales, String organizacionId,
            String nota) {
        boolean compatible = organizaciones.compatibles(ciudad, materiales).stream()
                .anyMatch(o -> o.id().equals(organizacionId));
        if (!compatible) {
            return Optional.empty();
        }
        var s = Solicitud.nueva(new SolicitudSemilla(actor.getCiudadanoId(),
                actor.getNombreCiudadano(), ciudad, direccion, referencia, materiales,
                organizacionId, nota, Estado.PENDIENTE, Instant.now(), null));
        repositorio.save(s);
        String nombre = organizaciones.buscar(organizacionId)
                .map(OrganizacionInfo::nombre).orElse(organizacionId);
        return Optional.of(aInfo(s, Map.of(organizacionId, nombre)));
    }

    @Transactional
    public Resultado aceptar(long id, long version) {
        return aplicar(id, version, this::esDestinataria, Solicitud::aceptar);
    }

    @Transactional
    public Resultado rechazar(long id, long version) {
        return aplicar(id, version, this::esDestinataria, s -> s.rechazar(Instant.now()));
    }

    @Transactional
    public Resultado completar(long id, long version) {
        return aplicar(id, version, this::esDestinataria, s -> s.completar(Instant.now()));
    }

    @Transactional
    public Resultado cancelar(long id, long version) {
        return aplicar(id, version, this::esAutor, s -> s.cancelar(Instant.now()));
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

    @Transactional
    public void reemplazarTodas(List<SolicitudSemilla> dataset) {
        repositorio.deleteAll();
        dataset.forEach(s -> repositorio.save(Solicitud.nueva(s)));
    }

    private Resultado aplicar(long id, long version, Predicate<Solicitud> permiso,
            Consumer<Solicitud> transicion) {
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
            return Resultado.OK;
        } catch (TransicionInvalidaException e) {
            return Resultado.INVALIDA;
        } catch (OptimisticLockingFailureException e) {
            return Resultado.CONFLICTO;
        }
    }

    private boolean esDestinataria(Solicitud s) {
        return actor.get() == Rol.ORGANIZACION
                && s.getOrganizacionId().equals(actor.getOrganizacionId());
    }

    private boolean esAutor(Solicitud s) {
        return actor.get() == Rol.CIUDADANO
                && s.getCiudadanoId().equals(actor.getCiudadanoId());
    }

    private static SolicitudInfo aInfo(Solicitud s, Map<String, String> nombres) {
        return new SolicitudInfo(s.getId(), s.getCiudad(), s.getDireccion(),
                s.getReferencia(), List.copyOf(s.getMateriales()),
                nombres.getOrDefault(s.getOrganizacionId(), s.getOrganizacionId()),
                s.getNombreCiudadano(), s.getNota(), s.getEstado(), s.getCreadaEn(),
                s.getFinalizadaEn(), s.getVersion());
    }
}
