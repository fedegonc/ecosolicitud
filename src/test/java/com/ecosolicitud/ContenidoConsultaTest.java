package com.ecosolicitud;

import java.nio.charset.StandardCharsets;

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
    @DisplayName("La portada muestra tres noticias reales con fuentes, fechas y fotos locales en ES/PT")
    void noticiasFrontera() {
        var navegador = new HtmlUnitDriver();
        try {
            String base = rest.getRootUri();
            for (String idioma : new String[]{"es", "pt"}) {
                navegador.get(base + "/?lang=" + idioma);
                var tarjetas = navegador.findElements(By.cssSelector(
                        ".noticias-comunidad .tarjeta"));
                assertThat(tarjetas).hasSize(3);
                assertThat(tarjetas.stream().map(t -> t.findElement(
                        By.tagName("time")).getAttribute("datetime")))
                        .containsExactly("2024-11-13", "2025-03-31", "2025-12-10");
                assertThat(tarjetas.getFirst().getText()).contains(idioma.equals("es")
                        ? "Novo Horizonte estrena" : "Novo Horizonte inaugura");
                for (var tarjeta : tarjetas) {
                    var foto = tarjeta.findElement(By.tagName("img"));
                    assertThat(foto.getAttribute("alt")).isNotBlank().doesNotContain("??");
                    assertThat(foto.getAttribute("src")).startsWith(base + "/img/comunidad/")
                            .endsWith(".webp");
                    var recurso = rest.getForEntity(foto.getAttribute("src"), byte[].class);
                    assertThat(recurso.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(new String(recurso.getBody(), 0, 4,
                            StandardCharsets.US_ASCII)).isEqualTo("RIFF");
                    var enlace = tarjeta.findElement(By.cssSelector(".noticia-fuente"));
                    assertThat(enlace.getAttribute("target")).isEqualTo("_blank");
                    assertThat(enlace.getAttribute("rel")).contains("noopener");
                    assertThat(enlace.getText()).isEqualTo(idioma.equals("es") ? "Ver más" : "Ver mais");
                    assertThat(tarjeta.getText()).contains("Foto:", "/ Unsplash")
                            .doesNotContain("Acopio Verde", "Cooperativa Frontera Limpia",
                                    "Campaña de electrónicos", "??");
                }
                assertThat(tarjetas.stream().map(t -> t.findElement(
                        By.cssSelector(".noticia-fuente")).getAttribute("href")))
                        .containsExactly(
                                "https://www.santanadolivramento.rs.gov.br/noticia/impressao?id=8560",
                                "https://americaretail-malls.com/paises/uruguay/comercios-de-rivera-impulsan-el-reciclaje-con-el-sello-verde/",
                                "https://www.rivera.gub.uy/portal/la-intendencia-de-rivera-inaugura-el-nuevo-punto-verde-2-en-el-polo-de-desarrollo/");
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
