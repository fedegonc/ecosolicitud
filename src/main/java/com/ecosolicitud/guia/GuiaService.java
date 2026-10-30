package com.ecosolicitud.guia;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ecosolicitud.guia.interno.Articulo;
import com.ecosolicitud.guia.interno.ArticuloRepository;
import com.ecosolicitud.shared.Markdown;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// La guía es de solo lectura para el ciudadano: hoy se carga con el dataset.
// La autoría (publicar un artículo) llega con la autenticación.
@Service
public class GuiaService {

    private final ArticuloRepository repositorio;
    private final Markdown markdown;

    // los artículos son contenido semilla: solo cambian en reemplazarTodos;
    // el cache evita también re-renderizar el markdown en cada pedido
    private volatile List<ArticuloInfo> cache;

    public GuiaService(ArticuloRepository repositorio, Markdown markdown) {
        this.repositorio = repositorio;
        this.markdown = markdown;
    }

    private List<ArticuloInfo> cargarTodos() {
        var c = cache;
        if (c == null) {
            c = repositorio.findAllByOrderByOrdenAsc().stream()
                    .map(this::aInfo).toList();
            cache = c;
        }
        return c;
    }

    public List<ArticuloInfo> articulos() {
        return cargarTodos();
    }

    public Optional<ArticuloInfo> buscar(long id) {
        // fallback al repo: una caché vieja nunca debe inventar un 404
        return cargarTodos().stream().filter(a -> a.id() == id).findFirst()
                .or(() -> repositorio.findById(id).map(this::aInfo));
    }

    public Optional<ArticuloInfo> buscarPorTitulo(String titulo) {
        return cargarTodos().stream().filter(a -> a.titulo().equals(titulo))
                .findFirst();
    }

    // la portada enlaza varios artículos por título: un solo IN en vez de
    // una consulta por cada tarjeta
    public Map<String, Long> idsDe(Collection<String> titulos) {
        var mapa = new HashMap<String, Long>();
        cargarTodos().stream().filter(a -> titulos.contains(a.titulo()))
                .forEach(a -> mapa.put(a.titulo(), a.id()));
        return mapa;
    }

    public boolean vacia() {
        return cargarTodos().isEmpty();
    }

    @Transactional
    public void reemplazarTodos(List<ArticuloSemilla> dataset) {
        repositorio.deleteAll();
        dataset.forEach(a -> repositorio.save(Articulo.nuevo(a)));
        cache = null;
    }

    private ArticuloInfo aInfo(Articulo a) {
        return new ArticuloInfo(a.getId(), a.getTitulo(), markdown.html(a.getCuerpo()),
                a.getImagen(), a.getMaterial(), a.getOrden());
    }
}
