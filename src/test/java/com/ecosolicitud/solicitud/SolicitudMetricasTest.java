package com.ecosolicitud.solicitud;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.ecosolicitud.shared.Material;
import com.ecosolicitud.solicitud.interno.Ciudadano;
import com.ecosolicitud.solicitud.interno.Solicitud;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SolicitudMetricasTest {

    private static final Instant AHORA = Instant.parse("2026-10-08T12:00:00Z");
    private static final Ciudadano VECINA = new Ciudadano("vecina", "Vecina");
    private static final List<Material> PLASTICO = List.of(new Material("PLASTICO", "Plástico", "botella"));

    private static Solicitud pendiente(long horasAtras) {
        return Solicitud.nueva(new SolicitudSemilla("vecina", "Vecina", "099", "Calle 1", null, null,
                PLASTICO, "centro", null, Estado.PENDIENTE, AHORA.minus(Duration.ofHours(horasAtras)),
                null, null), VECINA);
    }

    private static Instant mas(Solicitud s, long horas) {
        return s.getCreadaEn().plus(Duration.ofHours(horas));
    }

    // una de cada destino posible, con respuestas a las 2, 4, 6 y 8 h
    private static List<Solicitud> escenario() {
        var esperaDiez = pendiente(10);
        var esperaDos = pendiente(2);
        var enCurso = pendiente(60);
        enCurso.aceptar(mas(enCurso, 2));
        var completada = pendiente(60);
        completada.aceptar(mas(completada, 4));
        completada.completar(mas(completada, 20));
        var rechazadaDirecta = pendiente(60);
        rechazadaDirecta.rechazar(mas(rechazadaDirecta, 6));
        var aceptadaYRechazada = pendiente(60);
        aceptadaYRechazada.aceptar(mas(aceptadaYRechazada, 8));
        aceptadaYRechazada.rechazar(mas(aceptadaYRechazada, 30));
        var cancelada = pendiente(60);
        cancelada.cancelar(mas(cancelada, 1));
        return new ArrayList<>(List.of(esperaDiez, esperaDos, enCurso, completada,
                rechazadaDirecta, aceptadaYRechazada, cancelada));
    }

    @Test
    @DisplayName("Cada tasa divide solo por lo ya decidido")
    void definiciones() {
        var m = SolicitudMetricas.desde(escenario(), AHORA);

        assertThat(m.recibidas()).isEqualTo(7);
        assertThat(m.pendientes()).isEqualTo(2);
        assertThat(m.respondidas()).isEqualTo(4);
        assertThat(m.aceptadas()).as("la aceptada y después rechazada sigue contando").isEqualTo(3);
        assertThat(m.completadas()).isEqualTo(1);
        assertThat(m.aceptadasCerradas()).isEqualTo(2);
        assertThat(m.aceptacion()).isEqualTo("75%");
        assertThat(m.resolucion()).isEqualTo("50%");
        assertThat(m.mediana()).as("mediana de 2, 4, 6 y 8 h").isEqualTo("5 h");
        assertThat(m.espera()).as("la pendiente más vieja").isEqualTo("10 h");
    }

    @Test
    @DisplayName("Una solicitud nueva no mueve las tasas")
    void estabilidad() {
        var antes = SolicitudMetricas.desde(escenario(), AHORA);
        var conNueva = escenario();
        conNueva.add(pendiente(0));
        var despues = SolicitudMetricas.desde(conNueva, AHORA);

        assertThat(despues.recibidas()).isEqualTo(antes.recibidas() + 1);
        assertThat(despues.aceptacion()).isEqualTo(antes.aceptacion());
        assertThat(despues.resolucion()).isEqualTo(antes.resolucion());
        assertThat(despues.mediana()).isEqualTo(antes.mediana());
    }

    @Test
    @DisplayName("Sin datos se muestra un guion, no 0% ni NaN")
    void vacio() {
        var m = SolicitudMetricas.desde(List.of(), AHORA);
        assertThat(m).isEqualTo(SolicitudMetricas.ninguna());
        assertThat(List.of(m.aceptacion(), m.resolucion(), m.mediana(), m.espera()))
                .containsOnly("—");
    }

    @Test
    @DisplayName("Semillas: una rechazada se aceptó antes solo si la respuesta precede al cierre")
    void aceptacionDeSemillas() {
        var creada = AHORA.minus(Duration.ofDays(2));
        var respuesta = creada.plus(Duration.ofHours(3));
        var directa = semillaRechazada(creada, respuesta, respuesta);
        var trasAceptar = semillaRechazada(creada, respuesta, respuesta.plus(Duration.ofHours(5)));

        assertThat(directa.getAceptadaEn()).isNull();
        assertThat(trasAceptar.getAceptadaEn()).isEqualTo(respuesta);
    }

    private static Solicitud semillaRechazada(Instant creada, Instant respondida, Instant finalizada) {
        return Solicitud.nueva(new SolicitudSemilla("vecina", "Vecina", "099", "Calle 1", null, null,
                PLASTICO, "centro", null, Estado.RECHAZADA, creada, respondida, finalizada), VECINA);
    }
}
