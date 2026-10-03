package com.ecosolicitud.solicitud;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.solicitud.interno.Solicitud;
import com.ecosolicitud.solicitud.interno.SolicitudRepository;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
public class SolicitudService {

    private final SolicitudRepository repositorio;
    private final OrganizacionService organizaciones;

    public SolicitudService(SolicitudRepository repositorio,
            OrganizacionService organizaciones) {
        this.repositorio = repositorio;
        this.organizaciones = organizaciones;
    }

    public List<SolicitudInfo> misSolicitudes(String ciudadanoId, Filtro filtro) {
        var propias = repositorio.findByCiudadanoIdOrderByCreadaEnDesc(ciudadanoId).stream()
                .filter(s -> filtro.muestra(s.getEstado())).toList();
        var nombres = organizaciones.nombres(propias.stream()
                .map(Solicitud::getOrganizacionId).collect(toSet()));
        return propias.stream().map(s -> aInfo(s, nombres)).toList();
    }

    @Transactional
    public Optional<SolicitudInfo> crear(String ciudadanoId, Ciudad ciudad,
            String direccion, String referencia, List<Material> materiales,
            String organizacionId, String nota) {
        boolean compatible = organizaciones.compatibles(ciudad, materiales).stream()
                .anyMatch(o -> o.id().equals(organizacionId));
        if (!compatible) {
            return Optional.empty();
        }
        var s = new Solicitud();
        s.setCiudadanoId(ciudadanoId);
        s.setCiudad(ciudad);
        s.setDireccion(direccion);
        s.setReferencia(referencia);
        s.setMateriales(materiales);
        s.setOrganizacionId(organizacionId);
        s.setNota(nota);
        s.setEstado(Estado.PENDIENTE);
        s.setCreadaEn(Instant.now());
        repositorio.save(s);
        String nombre = organizaciones.buscar(organizacionId)
                .map(OrganizacionInfo::nombre).orElse(organizacionId);
        return Optional.of(aInfo(s, Map.of(organizacionId, nombre)));
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
        dataset.forEach(this::guardarNueva);
    }

    private void guardarNueva(SolicitudSemilla semilla) {
        var s = new Solicitud();
        s.setCiudadanoId(semilla.ciudadanoId());
        s.setCiudad(semilla.ciudad());
        s.setDireccion(semilla.direccion());
        s.setReferencia(semilla.referencia());
        s.setMateriales(semilla.materiales());
        s.setOrganizacionId(semilla.organizacionId());
        s.setNota(semilla.nota());
        s.setEstado(semilla.estado());
        s.setCreadaEn(semilla.creadaEn());
        s.setFinalizadaEn(semilla.finalizadaEn());
        repositorio.save(s);
    }

    private static SolicitudInfo aInfo(Solicitud s, Map<String, String> nombres) {
        return new SolicitudInfo(s.getId(), s.getCiudad(), s.getDireccion(),
                s.getReferencia(), List.copyOf(s.getMateriales()),
                nombres.getOrDefault(s.getOrganizacionId(), s.getOrganizacionId()),
                s.getNota(), s.getEstado(), s.getCreadaEn(), s.getFinalizadaEn(),
                s.getVersion());
    }
}
