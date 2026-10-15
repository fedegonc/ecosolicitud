package com.ecosolicitud;

import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.comunidad.interno.PublicacionRepository;
import com.ecosolicitud.guia.interno.ArticuloRepository;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
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
        var ficha = rest.getForEntity(Rutas.expandir(Rutas.GUIA_ARTICULO, a.getId()), String.class);
        assertThat(ficha.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ficha.getBody()).contains(a.getTitulo());
    }

    @Test
    @DisplayName("RF-7: /comunidad lista publicaciones y /comunidad/{id} muestra la ficha")
    void comunidad() {
        var lista = rest.getForEntity(Rutas.COMUNIDAD, String.class);
        assertThat(lista.getBody()).contains("Comunidad");
        var p = publicaciones.findAll().getFirst();
        var ficha = rest.getForEntity(Rutas.expandir(Rutas.COMUNIDAD_PUBLICACION, p.getId()), String.class);
        assertThat(ficha.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ficha.getBody()).contains(p.getTitulo());
    }

    @Test
    @DisplayName("La portada lista las tres últimas publicaciones de la comunidad, sin fotos")
    void ultimasComunidad() {
        var navegador = new HtmlUnitDriver();
        try {
            String base = rest.getRootUri();
            for (String idioma : new String[]{"es", "pt"}) {
                navegador.get(base + "/?lang=" + idioma);
                var seccion = navegador.findElement(By.id("ds-novedades"));
                var items = seccion.findElements(By.cssSelector(".novedades li"));
                assertThat(items).hasSize(3);
                for (var item : items) {
                    assertThat(item.findElement(By.tagName("a")).getAttribute("href"))
                            .startsWith(base + Rutas.COMUNIDAD + "/");
                    assertThat(item.findElement(By.tagName("time"))
                            .getAttribute("datetime")).isNotBlank();
                    assertThat(item.findElement(By.tagName("strong")).getText()).isNotBlank();
                    assertThat(item.findElement(By.cssSelector("a > span"))
                            .getText()).isNotBlank();
                }
                assertThat(seccion.findElements(By.tagName("img"))).isEmpty();
                assertThat(seccion.findElement(By.cssSelector(".cabecera a"))
                        .getAttribute("href")).isEqualTo(base + Rutas.COMUNIDAD);
            }
        } finally {
            navegador.quit();
        }
    }

    @Test
    @DisplayName("RF-8: /estadisticas responde con el resumen del servicio")
    void estadisticas() {
        var r = rest.getForEntity(Rutas.ESTADISTICAS, String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("Estadísticas del servicio")
                .contains("Por centro de acopio", "Centro de ejemplo – Paso del Enano",
                        "Centro de exemplo – Jardins", "periodo=todo")
                .doesNotContain("??");
        assertThat(rest.getForEntity(Rutas.ESTADISTICAS + "?periodo=invalido", String.class)
                .getStatusCode()).as("un período desconocido cae al de 30 días").isEqualTo(HttpStatus.OK);
    }
}
