package com.ecosolicitud.organizacion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ecosolicitud.organizacion.interno.Organizacion;
import com.ecosolicitud.organizacion.interno.OrganizacionRepository;
import com.ecosolicitud.shared.Actor;
import com.ecosolicitud.shared.AlmacenDelPedido;
import com.ecosolicitud.shared.CatalogoMateriales;
import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.shared.Memo;
import com.ecosolicitud.shared.Ubicacion;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Service
public class OrganizacionService {

    // en un mismo pedido la lista y la organización activa se consultan una
    // vez (el controller, los listados, el menú y los avisos las piden por
    // separado); al escribir se olvidan para que el resto del pedido relea
    private static final String MEMO = "organizacion:";
    private static final String MEMO_TODAS = MEMO + "todas";
    private static final String MEMO_NOMBRE = MEMO + "nombre:";

    private final OrganizacionRepository repositorio;
    private final CatalogoMateriales catalogo;

    public OrganizacionService(OrganizacionRepository repositorio,
            CatalogoMateriales catalogo) {
        this.repositorio = repositorio;
        this.catalogo = catalogo;
    }

    public List<OrganizacionInfo> todas() {
        return Memo.obtener(AlmacenDelPedido.actual(), MEMO_TODAS,
                () -> repositorio.findAll(Sort.by("nombre")).stream()
                        .map(OrganizacionService::aInfo).toList());
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
        return Memo.obtener(AlmacenDelPedido.actual(), MEMO + "actual:" + id, () -> {
            var encontrada = id != null ? buscar(id) : Optional.<OrganizacionInfo>empty();
            return encontrada.orElseGet(this::primera);
        });
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

    // cada lista de solicitudes pide los nombres de sus centros (la bandeja,
    // tres veces el mismo): en un pedido solo se consultan los que faltan
    public Map<String, String> nombres(Collection<String> ids) {
        var almacen = AlmacenDelPedido.actual();
        var nombres = new HashMap<String, String>();
        var faltan = new ArrayList<String>();
        for (var id : ids) {
            var guardado = almacen == null ? null : (String) almacen.leer(MEMO_NOMBRE + id);
            if (guardado != null) {
                nombres.put(id, guardado);
            } else {
                faltan.add(id);
            }
        }
        if (!faltan.isEmpty()) {
            repositorio.findAllById(faltan).forEach(o -> {
                nombres.put(o.getId(), o.getNombre());
                if (almacen != null) {
                    almacen.guardar(MEMO_NOMBRE + o.getId(), o.getNombre());
                }
            });
        }
        return nombres;
    }

    public boolean vacia() {
        return repositorio.count() == 0;
    }

    @Transactional
    public boolean actualizarPerfil(String id, List<Material> materiales,
            String horario, String telefono, Ubicacion ubicacion, long version) {
        var org = repositorio.findById(id).orElseThrow();
        if (org.getVersion() != version) {
            return false;
        }
        org.actualizar(catalogo.seleccionActiva(materiales), horario, telefono, ubicacion);
        repositorio.save(org);
        AlmacenDelPedido.olvidarTodo(MEMO);
        return true;
    }

    @Transactional
    public void reemplazarTodas(List<OrganizacionInfo> dataset) {
        repositorio.deleteAll();
        dataset.forEach(this::guardarNueva);
        AlmacenDelPedido.olvidarTodo(MEMO);
    }

    private void guardarNueva(OrganizacionInfo info) {
        repositorio.save(new Organizacion(info.id(), info.nombre(), info.ciudad(),
                info.materiales(), info.horario(), info.telefono(), info.ubicacion()));
    }

    private OrganizacionInfo primera() {
        return repositorio.findAll(Sort.by("id")).stream().findFirst()
                .map(OrganizacionService::aInfo).orElseThrow();
    }

    private static OrganizacionInfo aInfo(Organizacion o) {
        return new OrganizacionInfo(o.getId(), o.getNombre(), o.getCiudad(),
                List.copyOf(o.getMateriales()), o.getHorario(), o.getTelefono(),
                o.ubicacion(), o.getVersion());
    }
}
