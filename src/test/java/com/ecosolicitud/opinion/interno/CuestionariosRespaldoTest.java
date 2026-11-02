package com.ecosolicitud.opinion.interno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Locale;
import java.util.stream.Stream;

import com.ecosolicitud.shared.Rol;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CuestionariosRespaldoTest {

    private static Stream<Arguments> cuestionarios() {
        return Stream.of(
                Arguments.of(Rol.CIUDADANO, "es", 20, 18),
                Arguments.of(Rol.CIUDADANO, "pt", 20, 18),
                Arguments.of(Rol.ORGANIZACION, "es", 21, 19),
                Arguments.of(Rol.ORGANIZACION, "pt", 21, 19));
    }

    @ParameterizedTest
    @MethodSource("cuestionarios")
    void cargaConLaCantidadIndicada(Rol rol, String idioma, int total, int escalas) {
        var cuestionario = new CuestionariosRespaldo().para(rol, Locale.forLanguageTag(idioma));
        assertFalse(cuestionario.titulo().isBlank());
        assertEquals(total, cuestionario.preguntas().size());
        assertEquals(escalas, cuestionario.preguntas().stream()
                .filter(p -> p.tipo().name().equals("ESCALA")).count());
        assertEquals(1, cuestionario.preguntas().stream()
                .filter(p -> p.tipo().name().equals("TEXTO")).count());
        assertEquals(1, cuestionario.preguntas().stream()
                .filter(p -> p.tipo().name().equals("OPCION")).count());
    }

    @org.junit.jupiter.api.Test
    void esYPtDelMismoPerfilCoinciden() {
        // Son dos formularios bilingües: es y pt de un mismo perfil son el
        // mismo cuestionario traducido y deben tener igual estructura.
        for (Rol rol : Rol.values()) {
            var es = new CuestionariosRespaldo().para(rol, Locale.forLanguageTag("es"));
            var pt = new CuestionariosRespaldo().para(rol, Locale.forLanguageTag("pt"));
            assertEquals(es.preguntas().size(), pt.preguntas().size());
            for (int i = 0; i < es.preguntas().size(); i++) {
                assertEquals(es.preguntas().get(i).tipo(), pt.preguntas().get(i).tipo());
            }
        }
    }
}
