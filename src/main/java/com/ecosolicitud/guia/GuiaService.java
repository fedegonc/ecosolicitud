package com.ecosolicitud.guia;

import java.util.List;
import java.util.Optional;

import com.ecosolicitud.guia.interno.Articulo;
import com.ecosolicitud.guia.interno.ArticuloRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// La guía es de solo lectura para el ciudadano: hoy se carga con el dataset.
// La autoría (publicar un artículo) llega con la autenticación.
@Service
public class GuiaService {

    private final ArticuloRepository repositorio;

    public GuiaService(ArticuloRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<ArticuloInfo> articulos() {
        return repositorio.findAllByOrderByOrdenAsc().stream()
                .map(GuiaService::aInfo).toList();
    }

    public Optional<ArticuloInfo> buscar(long id) {
        return repositorio.findById(id).map(GuiaService::aInfo);
    }

    public boolean vacia() {
        return repositorio.count() == 0;
    }

    @Transactional
    public void reemplazarTodos(List<ArticuloSemilla> dataset) {
        repositorio.deleteAll();
        dataset.forEach(a -> repositorio.save(Articulo.nuevo(a)));
    }

    private static ArticuloInfo aInfo(Articulo a) {
        return new ArticuloInfo(a.getId(), a.getTitulo(), a.getCuerpo(), a.getMaterial(),
                a.getOrden());
    }
}
