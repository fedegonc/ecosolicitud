package com.ecosolicitud.organizacion;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ecosolicitud.organizacion.interno.Organizacion;
import com.ecosolicitud.organizacion.interno.OrganizacionRepository;
import com.ecosolicitud.shared.Actor;
import com.ecosolicitud.shared.CatalogoMateriales;
import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Service
public class OrganizacionService {

    private final OrganizacionRepository repositorio;
    private final CatalogoMateriales catalogo;

    public OrganizacionService(OrganizacionRepository repositorio,
            CatalogoMateriales catalogo) {
        this.repositorio = repositorio;
        this.catalogo = catalogo;
    }

    public List<OrganizacionInfo> todas() {
        return repositorio.findAll(Sort.by("nombre")).stream()
                .map(OrganizacionService::aInfo).toList();
    }

    public List<AcopiosCiudad> porCiudad() {
        var agrupadas = todas().stream().collect(groupingBy(OrganizacionInfo::ciudad,
                () -> new EnumMap<>(Ciudad.class), toList()));
        return Arrays.stream(Ciudad.values())
                .map(c -> new AcopiosCiudad(c, agrupadas.getOrDefault(c, List.of())))
                .toList();
    }

    public OrganizacionInfo actual(Actor actor) {
        String id = actor.organizacionId();
        var encontrada = id != null ? buscar(id) : Optional.<OrganizacionInfo>empty();
        return encontrada.orElseGet(this::primera);
    }

    public Optional<OrganizacionInfo> buscar(String id) {
        return repositorio.findById(id).map(OrganizacionService::aInfo);
    }

    public List<OrganizacionInfo> compatibles(Ciudad ciudad,
            Collection<Material> materiales) {
        return repositorio.findByCiudadOrderByNombre(ciudad).stream()
                .map(OrganizacionService::aInfo)
                .filter(o -> o.recibeTodos(materiales)).toList();
    }

    public List<OrganizacionInfo> queReciben(Material material) {
        return repositorio.findAll(Sort.by("nombre")).stream()
                .filter(o -> o.getMateriales().contains(material))
                .map(OrganizacionService::aInfo).toList();
    }

    public List<OrganizacionInfo> enCiudad(Ciudad ciudad) {
        return repositorio.findByCiudadOrderByNombre(ciudad).stream()
                .map(OrganizacionService::aInfo).toList();
    }

    public Map<String, String> nombres(Collection<String> ids) {
        return repositorio.findAllById(ids).stream()
                .collect(toMap(Organizacion::getId, Organizacion::getNombre));
    }

    public boolean vacia() {
        return repositorio.count() == 0;
    }

    @Transactional
    public boolean actualizarPerfil(String id, List<Material> materiales,
            String horario, String telefono, long version) {
        var org = repositorio.findById(id).orElseThrow();
        if (org.getVersion() != version) {
            return false;
        }
        org.actualizar(catalogo.seleccionActiva(materiales), horario, telefono);
        repositorio.save(org);
        return true;
    }

    @Transactional
    public void reemplazarTodas(List<OrganizacionInfo> dataset) {
        repositorio.deleteAll();
        dataset.forEach(this::guardarNueva);
    }

    private void guardarNueva(OrganizacionInfo info) {
        repositorio.save(new Organizacion(info.id(), info.nombre(), info.ciudad(),
                info.materiales(), info.horario(), info.telefono()));
    }

    private OrganizacionInfo primera() {
        return repositorio.findAll(Sort.by("id")).stream().findFirst()
                .map(OrganizacionService::aInfo).orElseThrow();
    }

    private static OrganizacionInfo aInfo(Organizacion o) {
        return new OrganizacionInfo(o.getId(), o.getNombre(), o.getCiudad(),
                List.copyOf(o.getMateriales()), o.getHorario(), o.getTelefono(), o.getVersion());
    }
}
