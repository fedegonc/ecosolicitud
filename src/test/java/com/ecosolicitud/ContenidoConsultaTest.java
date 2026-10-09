package com.ecosolicitud;

import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.comunidad.interno.PublicacionRepository;
import com.ecosolicitud.guia.interno.ArticuloRepository;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Epic("Consulta de contenido")
@Feature("Avisos, guía, comunidad y estadísticas responden (RF-5 a RF-8)")
class ContenidoConsultaTest {

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private ArticuloRepository articulos;
    @Autowired
    private PublicacionRepository publicaciones;

    @Test
    @DisplayName("RF-5: /avisos responde con la bandeja del actor")
    void avisos() {
        var r = rest.getForEntity(Rutas.AVISOS, String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("Avisos");
    }

    @Test
    @DisplayName("RF-6: /guia lista artículos y /guia/{id} muestra la ficha")
    void guia() {
        var lista = rest.getForEntity(Rutas.GUIA, String.class);
        assertThat(lista.getBody()).contains("Guía de reciclaje");
        var a = articulos.findAll().getFirst();
        var ficha = rest.getForEntity(Rutas.GUIA + "/" + a.getId(), String.class);
        assertThat(ficha.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ficha.getBody()).contains(a.getTitulo());
    }

    @Test
    @DisplayName("RF-7: /comunidad lista publicaciones y /comunidad/{id} muestra la ficha")
    void comunidad() {
        var lista = rest.getForEntity(Rutas.COMUNIDAD, String.class);
        assertThat(lista.getBody()).contains("Comunidad");
        var p = publicaciones.findAll().getFirst();
        var ficha = rest.getForEntity(Rutas.COMUNIDAD + "/" + p.getId(), String.class);
        assertThat(ficha.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ficha.getBody()).contains(p.getTitulo());
    }

    @Test
    @DisplayName("RF-8: /estadisticas responde con el resumen del servicio")
    void estadisticas() {
        var r = rest.getForEntity(Rutas.ESTADISTICAS, String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("Estadísticas del servicio")
                .contains("Por centro de acopio", "Cooperativa Frontera Limpia",
                        "Coleta Solidária Livramento", "periodo=todo")
                .doesNotContain("??");
        assertThat(rest.getForEntity(Rutas.ESTADISTICAS + "?periodo=invalido", String.class)
                .getStatusCode()).as("un período desconocido cae al de 30 días").isEqualTo(HttpStatus.OK);
    }
}
