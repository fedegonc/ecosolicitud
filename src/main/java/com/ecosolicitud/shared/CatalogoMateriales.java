package com.ecosolicitud.shared;

import java.util.List;

import com.ecosolicitud.shared.interno.MaterialRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Catálogo único: toda selección de materiales pasa por acá.
// "Solo activos" es la regla de las nuevas selecciones (formularios y servicios);
// el historial siempre puede resolver cualquier código, incluso desactivado.
@Service
public class CatalogoMateriales {

    private static final List<Material> SEMILLA = List.of(
            new Material("PLASTICO", "Plástico", "botella"),
            new Material("CARTON", "Cartón", "caja"),
            new Material("PAPEL", "Papel", "hoja"),
            new Material("VIDRIO", "Vidrio", "frasco"),
            new Material("METAL", "Metal", "lata"),
            new Material("ELECTRONICOS", "Electrónicos", "chip"),
            new Material("LATAS", "Latas", "lata"));

    private final MaterialRepository repositorio;

    // el catálogo es semilla fija: solo cambia en semillarFaltantes
    private volatile List<Material> activosCache;
    private volatile java.util.Map<String, Material> porCodigo;

    public CatalogoMateriales(MaterialRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<Material> activos() {
        var c = activosCache;
        if (c == null) {
            c = List.copyOf(repositorio.findByActivoTrueOrderById());
            activosCache = c;
        }
        return c;
    }

    public List<Material> todos() {
        return repositorio.findAll();
    }

    public Material resolver(String codigo) {
        var mapa = porCodigo;
        if (mapa == null) {
            var nuevo = new java.util.HashMap<String, Material>();
            repositorio.findAll().forEach(m -> nuevo.put(m.getCodigo(), m));
            porCodigo = nuevo;
            mapa = nuevo;
        }
        var m = mapa.get(codigo);
        if (m == null) {
            throw new IllegalArgumentException("Material desconocido: " + codigo);
        }
        return m;
    }

    // El formulario trae entidades detached (o semillas de tests): las
    // normaliza a entidades gestionadas por código. Exige activos porque
    // se usa en el camino de nuevas selecciones (crear/perfil).
    public List<Material> seleccionActiva(java.util.Collection<Material> pedidos) {
        var resueltos = pedidos.stream().map(m -> resolver(m.getCodigo())).toList();
        if (resueltos.stream().anyMatch(m -> !m.isActivo())) {
            throw new IllegalArgumentException("Material inactivo seleccionado");
        }
        return resueltos;
    }

    @Transactional
    public void semillarFaltantes() {
        SEMILLA.stream().filter(m -> repositorio.findByCodigo(m.getCodigo()).isEmpty())
                .forEach(m -> repositorio.save(new Material(m.getCodigo(), m.getNombre(), m.getIcono())));
        activosCache = null;
        porCodigo = null;
    }
}
