package com.ecosolicitud;

import com.ecosolicitud.shared.Rutas;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.By;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OpinionInteraccionTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private HtmlUnitDriver driver;
    private String base;

    @BeforeEach
    void preparar() {
        driver = new HtmlUnitDriver();
        base = "http://localhost:" + port;
        AccesoDemoTest.entrar(driver, base);
    }

    @AfterEach
    void cerrar() {
        driver.quit();
    }

    @ParameterizedTest
    @CsvSource({"CIUDADANO,es", "CIUDADANO,pt", "ORGANIZACION,es", "ORGANIZACION,pt"})
    void respaldoLocalAntesDeGoogleYEnlacesDirectos(String rol, String idioma) {
        if (rol.equals("ORGANIZACION")) {
            driver.findElement(By.cssSelector(".rol-switch button[value='ORGANIZACION']")).click();
        }
        driver.get(base + Rutas.OPINION + "?lang=" + idioma);
        var respaldo = driver.findElement(By.id("respaldo"));
        assertThat(respaldo.getTagName()).isEqualTo("details");
        assertThat(respaldo.findElement(By.tagName("summary")).getDomAttribute("hx-get")).isNull();
        assertThat(respaldo.findElements(By.cssSelector("form input[type='radio']"))).isNotEmpty();
        var html = driver.getPageSource();
        assertThat(html.indexOf("id=\"respaldo\"")).isLessThan(html.indexOf("<iframe"));
        assertThat(html).doesNotContain("??opinion.");
        assertThat(respaldo.getText()).contains(idioma.equals("es")
                ? "Responder dentro de EcoSolicitud" : "Responder dentro do EcoSolicitud");
        var enlace = driver.findElement(By.cssSelector(".cuestionario-externo"));
        var url = enlace.getDomAttribute("href");
        assertThat(url).startsWith("https://docs.google.com/forms/d/e/").endsWith("/viewform");
        assertThat(enlace.getDomAttribute("hx-boost")).isEqualTo("false");
        assertThat(driver.findElement(By.tagName("iframe")).getDomAttribute("src"))
                .isEqualTo(url + "?embedded=true");
    }

    @Test
    void respaldoConservaValidacionYGuardaSinDependerDeGoogle() {
        driver.get(base + Rutas.OPINION);
        driver.findElement(By.cssSelector("#respaldo summary")).click();
        driver.findElement(By.cssSelector("#respaldo button[type='submit']")).click();
        assertThat(driver.getCurrentUrl()).contains("respaldo=incompleto");
        assertThat(driver.findElement(By.id("respaldo")).getDomAttribute("open")).isNotNull();
        long antes = respuestas();
        driver.findElement(By.cssSelector("#respaldo input[type='radio'][value='5']")).click();
        driver.findElement(By.cssSelector("#respaldo button[type='submit']")).click();
        assertThat(driver.getCurrentUrl()).contains("respaldo=gracias");
        assertThat(respuestas()).isEqualTo(antes + 1);
        assertThat(driver.findElements(By.cssSelector("#respaldo form"))).isEmpty();
    }

    private long respuestas() {
        return jdbc.queryForObject("select count(*) from respuestas_respaldo", Long.class);
    }
}
