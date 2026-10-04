package com.ecosolicitud.comunidad;

import java.util.List;
import java.util.Optional;

import com.ecosolicitud.comunidad.interno.Publicacion;
import com.ecosolicitud.comunidad.interno.PublicacionRepository;
import com.ecosolicitud.comunidad.interno.Reciclador;
import com.ecosolicitud.comunidad.interno.RecicladorRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Solo lectura para el ciudadano: hoy el contenido se carga con el dataset.
// Publicar llega con la autenticación (y con ella, la moderación).
@Service
public class ComunidadService {

    // el contenido crece por años: la sección muestra lo último, no todo
    private static final int ULTIMAS = 10;

    private final PublicacionRepository repositorio;
    private final RecicladorRepository recicladores;

    public ComunidadService(PublicacionRepository repositorio,
            RecicladorRepository recicladores) {
        this.repositorio = repositorio;
        this.recicladores = recicladores;
    }

    public List<PublicacionInfo> publicadas(FiltroComunidad filtro) {
        return repositorio.findAllByOrderByPublicadaEnDesc().stream()
                .filter(p -> filtro.muestra(p.getTipo()))
                .limit(ULTIMAS)
                .map(ComunidadService::aInfo).toList();
    }

    public List<PublicacionInfo> ultimas(int cantidad) {
        return publicadas(FiltroComunidad.TODAS).stream()
                .limit(cantidad).toList();
    }

    public Optional<PublicacionInfo> buscar(long id) {
        return repositorio.findById(id).map(ComunidadService::aInfo);
    }

    public boolean vacia() {
        return repositorio.count() == 0;
    }

    public List<RecicladorInfo> recicladores() {
        return recicladores.findAllByOrderByNombre().stream()
                .map(r -> new RecicladorInfo(r.getNombre(), r.getTelefono())).toList();
    }

    @Transactional
    public void reemplazarRecicladores(List<RecicladorInfo> dataset) {
        recicladores.deleteAllInBatch();
        dataset.forEach(r -> recicladores.save(new Reciclador(r.nombre(), r.telefono())));
    }

    @Transactional
    public void reemplazarTodas(List<PublicacionSemilla> dataset) {
        repositorio.deleteAll();
        dataset.forEach(p -> repositorio.save(Publicacion.nueva(p)));
    }

    private static PublicacionInfo aInfo(Publicacion p) {
        return new PublicacionInfo(p.getId(), p.getTitulo(), p.getResumen(), p.getCuerpo(),
                p.getTipo(), p.getPublicadaEn());
    }
}
