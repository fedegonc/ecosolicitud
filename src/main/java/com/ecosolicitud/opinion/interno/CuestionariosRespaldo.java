package com.ecosolicitud.opinion.interno;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.ecosolicitud.shared.Rol;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

// Carga los cuatro cuestionarios al arrancar: un archivo mal escrito falla el inicio, no la encuesta.
@Component
class CuestionariosRespaldo {

    private static final List<String> IDIOMAS = List.of("es", "pt");

    private final Map<String, Cuestionario> porClave = new HashMap<>();

    CuestionariosRespaldo() {
        for (Rol rol : Rol.values()) {
            for (String idioma : IDIOMAS) {
                String clave = clave(rol, idioma);
                var recurso = new ClassPathResource("cuestionarios/" + clave + ".txt");
                try {
                    porClave.put(clave, Cuestionario.leer(
                            recurso.getContentAsString(StandardCharsets.UTF_8).lines().toList()));
                } catch (IOException e) {
                    throw new UncheckedIOException("No se pudo leer " + recurso.getPath(), e);
                }
            }
        }
    }

    Cuestionario para(Rol rol, Locale locale) {
        return porClave.get(clave(rol, idioma(locale)));
    }

    static String idioma(Locale locale) {
        return "pt".equals(locale.getLanguage()) ? "pt" : "es";
    }

    private static String clave(Rol rol, String idioma) {
        return rol.name().toLowerCase(Locale.ROOT) + "-" + idioma;
    }
}
