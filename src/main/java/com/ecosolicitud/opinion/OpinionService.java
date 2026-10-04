package com.ecosolicitud.opinion;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public int registrar(Map<String, Integer> valoraciones, String comentario,
            Rol rol) {
        String texto = comentario == null || comentario.isBlank() ? null
                : comentario.trim();
        var ahora = Instant.now();
        var validas = valoraciones.entrySet().stream()
                .filter(e -> e.getKey() != null
                        && e.getKey().matches("[A-Z_]{1,20}")
                        && e.getValue() != null && e.getValue() >= 1
                        && e.getValue() <= 5)
                .toList();
        validas.forEach(e -> repositorio.save(
                new Opinion(e.getKey(), e.getValue(), texto, rol, ahora)));
        return validas.size();
    }

    public Double promedio() {
        return repositorio.promedio();
    }

    public long cantidad() {
        return repositorio.count();
    }

    public Map<String, Double> promedioPorSeccion() {
        var promedios = new LinkedHashMap<String, Double>();
        repositorio.promedioPorSeccion()
                .forEach(f -> promedios.put((String) f[0], (Double) f[1]));
        return promedios;
    }

    @Transactional
    public void reemplazarTodas(List<OpinionSemilla> dataset) {
        repositorio.deleteAll();
        dataset.forEach(s -> repositorio.save(new Opinion(s.seccion(),
                s.valor(), s.comentario(), s.rol(), s.creadaEn())));
    }
}
