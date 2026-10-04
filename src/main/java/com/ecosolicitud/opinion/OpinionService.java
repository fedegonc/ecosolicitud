package com.ecosolicitud.opinion;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecosolicitud.opinion.interno.Opinion;
import com.ecosolicitud.opinion.interno.OpinionRepository;
import com.ecosolicitud.shared.Rol;

// Recoge y resume las valoraciones de facilidad de uso.
@Service
@Transactional(readOnly = true)
public class OpinionService {

    private final OpinionRepository repositorio;

    public OpinionService(OpinionRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public void registrar(int valor, String comentario, Rol rol) {
        String texto = comentario == null || comentario.isBlank() ? null
                : comentario.trim();
        repositorio.save(new Opinion(valor, texto, rol, Instant.now()));
    }

    public Double promedio() {
        return repositorio.promedio();
    }

    public long cantidad() {
        return repositorio.count();
    }

    @Transactional
    public void reemplazarTodas(List<OpinionSemilla> dataset) {
        repositorio.deleteAll();
        dataset.forEach(s -> repositorio.save(
                new Opinion(s.valor(), s.comentario(), s.rol(), s.creadaEn())));
    }
}
