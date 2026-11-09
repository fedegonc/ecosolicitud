package com.ecosolicitud;

import com.ecosolicitud.shared.Rutas;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:catalogo-test;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"dev", "test"})
class DesignSystemTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private final HtmlUnitDriver driver = new HtmlUnitDriver();

    @AfterEach
    void cerrar() {
        driver.quit();
    }

    @ParameterizedTest
    @ValueSource(strings = {"es", "pt"})
    void catalogoCompactoTraducidoConKanbanCompartido(String idioma) {
        driver.get(base() + Rutas.DEV_DESIGN_SYSTEM + "?lang=" + idioma);
        assertThat(driver.findElements(By.cssSelector("main > details"))).hasSize(4);
        assertThat(driver.findElements(By.cssSelector("main > details[name='catalogo-seccion']")))
                .hasSize(4);
        assertThat(driver.findElements(By.cssSelector("main > details[open]"))).hasSize(1);
        assertThat(driver.findElements(By.cssSelector("#patrones .kanban-col"))).hasSize(2);
        assertThat(driver.findElements(By.cssSelector("#patrones .kanban .tarjeta"))).hasSize(1);
        assertThat(driver.findElements(By.cssSelector("#patrones > .fila .etiqueta"))).hasSize(5);
        assertThat(driver.findElements(By.cssSelector("iframe, style"))).isEmpty();
        assertThat(driver.getPageSource()).doesNotContain("??", "catalogo-listado", "catalogo-externo");
        assertThat(driver.findElement(By.tagName("html")).getDomAttribute("lang")).isEqualTo(idioma);
        assertThat(driver.findElement(By.cssSelector("#reglas summary")).getDomProperty("textContent"))
                .contains(idioma.equals("es") ? "Reglas de uso" : "Regras de uso");
        var hojas = driver.findElements(By.cssSelector("link[rel='stylesheet']"));
        assertThat(hojas).hasSize(1);
        assertThat(hojas.getFirst().getDomAttribute("href")).endsWith("/css/app.css");
    }

    @Test
    void visitarLasMuestrasNoModificaLosDatos() {
        long solicitudes = contar("solicitudes");
        long usuarios = contar("usuarios");
        long respuestas = contar("respuestas_respaldo");
        driver.get(base() + Rutas.DEV_DESIGN_SYSTEM);
        assertThat(contar("solicitudes")).isEqualTo(solicitudes);
        assertThat(contar("usuarios")).isEqualTo(usuarios);
        assertThat(contar("respuestas_respaldo")).isEqualTo(respuestas);
        var formulario = driver.findElement(By.id("catalogo-form"));
        assertThat(formulario.getDomAttribute("method")).isEqualTo("get");
        assertThat(formulario.getDomAttribute("action")).endsWith(Rutas.DEV_DESIGN_SYSTEM);
    }

    private String base() {
        return "http://localhost:" + port;
    }

    private long contar(String tabla) {
        return jdbc.queryForObject("select count(*) from " + tabla, Long.class);
    }
}
