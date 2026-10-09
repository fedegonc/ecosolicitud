package com.ecosolicitud;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.solicitud.Estado;
import com.ecosolicitud.solicitud.SolicitudSemilla;
import com.ecosolicitud.solicitud.interno.Ciudadano;
import com.ecosolicitud.solicitud.TransicionInvalidaException;
import com.ecosolicitud.solicitud.interno.Solicitud;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Epic("Etapa 5: máquina de estados")
@Feature("Transiciones de Solicitud sin Spring")
class SolicitudMaquinaTest {

    private static final Instant CREADA = Instant.parse("2026-10-01T12:00:00Z");
    private static final Instant RELOJ = Instant.parse("2026-10-03T12:00:00Z");

    private static Solicitud en(Estado estado) {
        boolean respondida = estado != Estado.PENDIENTE && estado != Estado.CANCELADA;
        return Solicitud.nueva(new SolicitudSemilla("c", "N", "Tel", "Dir 1",
                null, null, List.of(mat("PLASTICO")), "org", null, estado, CREADA,
                respondida ? CREADA : null,
                estado.esFinal() ? CREADA.plus(1, ChronoUnit.HOURS) : null), ciudadano());
    }

    private static Material mat(String codigo) {
        return new Material(codigo, codigo, "botella");
    }

    private static Ciudadano ciudadano() {
        return new Ciudadano("c", "N");
    }

    private static void aplicar(Solicitud s, String accion) {
        switch (accion) {
            case "aceptar" -> s.aceptar(RELOJ);
            case "rechazar" -> s.rechazar(RELOJ);
            case "completar" -> s.completar(RELOJ);
            case "cancelar" -> s.cancelar(RELOJ);
            default -> throw new IllegalArgumentException(accion);
        }
    }

    private static boolean valida(Estado desde, String accion) {
        return switch (accion) {
            case "aceptar" -> desde == Estado.PENDIENTE;
            case "rechazar" -> desde == Estado.PENDIENTE || desde == Estado.EN_CURSO;
            case "completar" -> desde == Estado.EN_CURSO;
            case "cancelar" -> desde == Estado.PENDIENTE;
            default -> false;
        };
    }

    private static Estado destino(String accion) {
        return switch (accion) {
            case "aceptar" -> Estado.EN_CURSO;
            case "rechazar" -> Estado.RECHAZADA;
            case "completar" -> Estado.COMPLETADA;
            case "cancelar" -> Estado.CANCELADA;
            default -> null;
        };
    }

    static Stream<Arguments> casos() {
        var acciones = new String[]{"aceptar", "rechazar", "completar", "cancelar"};
        return Stream.of(Estado.values()).flatMap(desde ->
                Stream.of(acciones).map(accion -> Arguments.of(desde, accion)));
    }

    @ParameterizedTest(name = "{0} + {1}")
    @MethodSource("casos")
    @DisplayName("Las 20 combinaciones: válidas transicionan, inválidas no tocan nada")
    void transiciones(Estado desde, String accion) {
        var s = en(desde);
        var finAntes = s.getFinalizadaEn();
        if (valida(desde, accion)) {
            aplicar(s, accion);
            var esperado = destino(accion);
            assertThat(s.getEstado()).isEqualTo(esperado);
            assertThat(s.getFinalizadaEn() != null).isEqualTo(esperado.esFinal());
            if (esperado.esFinal()) {
                assertThat(s.getFinalizadaEn()).isEqualTo(RELOJ);
                assertThat(s.getFinalizadaEn()).isAfterOrEqualTo(s.getCreadaEn());
            }
        } else {
            assertThatThrownBy(() -> aplicar(s, accion))
                    .isInstanceOf(TransicionInvalidaException.class);
            assertThat(s.getEstado()).isEqualTo(desde);
            assertThat(s.getFinalizadaEn()).isEqualTo(finAntes);
        }
    }

    @Test
    @DisplayName("Semilla inconsistente con I1 o I2 es rechazada al construir")
    void invariantesEnConstruccion() {
        assertThatThrownBy(() -> Solicitud.nueva(new SolicitudSemilla("c", "N",
                "Tel", "Dir", null, null, List.of(mat("PAPEL")), "org", null,
                Estado.COMPLETADA, CREADA, CREADA, null), ciudadano()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Solicitud.nueva(new SolicitudSemilla("c", "N",
                "Tel", "Dir", null, null, List.of(mat("PAPEL")), "org", null,
                Estado.COMPLETADA, CREADA, CREADA, CREADA.minus(1, ChronoUnit.HOURS)), ciudadano()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
