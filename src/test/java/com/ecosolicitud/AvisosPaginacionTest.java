package com.ecosolicitud;

import java.time.Instant;
import java.util.stream.IntStream;

import com.ecosolicitud.demo.DemoService;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.solicitud.Estado;
import com.ecosolicitud.solicitud.SolicitudService;
import com.ecosolicitud.solicitud.TipoAviso;
import com.ecosolicitud.solicitud.interno.Aviso;
import com.ecosolicitud.solicitud.interno.AvisoRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AvisosPaginacionTest {

    private static final String VECINO = "ciudadano-demo";
    private static final String CENTRO = "frontera-limpia";

    @LocalServerPort
    private int port;

    @Autowired
    private DemoService demo;

    @Autowired
    private AvisoRepository avisos;

    @Autowired
    private SolicitudService solicitudes;

    private HtmlUnitDriver driver;
    private String base;

    @BeforeEach
    void preparar() {
        demo.reiniciar();
        avisos.deleteAll();
        agregar(VECINO);
        agregar(CENTRO);
        driver = new HtmlUnitDriver();
        base = "http://localhost:" + port;
        AccesoDemoTest.entrar(driver, base);
    }

    @AfterEach
    void cerrar() {
        driver.quit();
    }

    @Test
    void paginaSinRepetirNiMarcarLosAvisosNoMostrados() {
        driver.get(base + Rutas.AVISOS);
        assertThat(driver.findElements(By.cssSelector(".timeline > li"))).hasSize(12);
        assertThat(driver.findElements(By.cssSelector(".timeline > li.nueva"))).hasSize(12);
        assertThat(driver.findElement(By.cssSelector(".timeline > li")).getText())
                .contains("Centro prueba 25");
        assertThat(driver.findElement(By.cssSelector(".paginacion")).getText())
                .contains("Página 1 de 3");
        assertThat(avisos.countByDestinoAndLeidaFalse(VECINO)).isEqualTo(13);
        assertThat(avisos.countByDestinoAndLeidaFalse(CENTRO)).isEqualTo(25);
        assertThat(driver.findElement(By.cssSelector(".badge")).getDomAttribute("data-n"))
                .isEqualTo("13");

        driver.findElement(By.cssSelector(".paginacion a[href$='pagina=2']")).click();
        assertThat(driver.findElements(By.cssSelector(".timeline > li"))).hasSize(12);
        assertThat(driver.findElement(By.cssSelector(".timeline > li")).getText())
                .contains("Centro prueba 13");
        assertThat(avisos.countByDestinoAndLeidaFalse(VECINO)).isEqualTo(1);
        driver.findElement(By.cssSelector(".paginacion a[href$='pagina=3']")).click();
        assertThat(driver.findElements(By.cssSelector(".timeline > li"))).hasSize(1);
        assertThat(driver.findElement(By.cssSelector(".timeline > li")).getText())
                .contains("Centro prueba 1");
        assertThat(avisos.countByDestinoAndLeidaFalse(VECINO)).isZero();

        driver.findElement(By.cssSelector(".paginacion a")).click();
        assertThat(driver.findElements(By.cssSelector(".timeline > li.nueva"))).isEmpty();
        assertThat(avisos.countByDestinoAndLeidaFalse(CENTRO)).isEqualTo(25);
    }

    @Test
    void corrigePaginasFueraDeRango() {
        driver.get(base + Rutas.AVISOS + "?pagina=-2147483648");
        assertThat(driver.findElements(By.cssSelector(".timeline > li"))).hasSize(12);
        assertThat(driver.findElement(By.cssSelector(".paginacion")).getText())
                .contains("Página 1 de 3");
        driver.get(base + Rutas.AVISOS + "?pagina=2147483647");
        assertThat(driver.findElements(By.cssSelector(".timeline > li"))).hasSize(1);
        assertThat(driver.findElement(By.cssSelector(".paginacion")).getText())
                .contains("Página 3 de 3");
        assertThat(avisos.countByDestinoAndLeidaFalse(VECINO)).isEqualTo(12);
    }

    @Test
    void centroAgrupaLaPaginaPorEstadoActualSinLeerOtrosDestinatarios() {
        var completada = solicitudes.todas().stream()
                .filter(s -> s.estado() == Estado.COMPLETADA).findFirst().orElseThrow();
        avisos.save(new Aviso(CENTRO, completada.id(), TipoAviso.NUEVA,
                "Vecino prueba", Instant.now()));
        driver.findElement(By.cssSelector(".rol-switch button[value='ORGANIZACION']")).click();
        new Select(driver.findElement(By.cssSelector(".org-switch select"))).selectByValue(CENTRO);
        driver.findElement(By.cssSelector(".org-switch button")).click();

        driver.get(base + Rutas.AVISOS);
        var columnas = driver.findElements(By.cssSelector(".kanban-col"));
        assertThat(columnas).hasSize(2);
        assertThat(driver.findElements(By.cssSelector(".kanban .tarjeta"))).hasSize(12);
        assertThat(columnas.get(1).getText()).contains("Vecino prueba");
        assertThat(columnas.get(1).findElements(By.cssSelector(".est-completada"))).hasSize(1);
        assertThat(avisos.countByDestinoAndLeidaFalse(CENTRO)).isEqualTo(14);
        assertThat(avisos.countByDestinoAndLeidaFalse(VECINO)).isEqualTo(25);
    }

    @Test
    void paginacionEnPortuguesConservaElIdiomaAlNavegar() {
        driver.get(base + Rutas.AVISOS + "?lang=pt");
        assertThat(driver.findElement(By.cssSelector(".paginacion")).getText())
                .contains("Próxima", "Página 1 de 3");
        assertThat(driver.getPageSource()).contains("Os avisos desta página");
        driver.findElement(By.cssSelector(".paginacion a")).click();
        assertThat(driver.findElement(By.cssSelector(".paginacion")).getText())
                .contains("Próxima", "Página 2 de 3");
        assertThat(driver.getPageSource()).doesNotContain("??avisos.", "??paginacion.");
    }

    @Test
    void bandejaVaciaNoTienePaginacionNiMarcaAvisosDeOtroDestino() {
        avisos.deleteAll();
        agregar(CENTRO);
        driver.get(base + Rutas.AVISOS);
        assertThat(driver.findElement(By.tagName("main")).getText()).contains("Sin avisos");
        assertThat(driver.findElements(By.cssSelector(".paginacion, .timeline"))).isEmpty();
        assertThat(avisos.countByDestinoAndLeidaFalse(CENTRO)).isEqualTo(25);
    }

    private void agregar(String destino) {
        // Fechas iguales: el id desempata para no repetir avisos entre páginas.
        var fecha = Instant.parse("2026-01-01T12:00:00Z");
        avisos.saveAll(IntStream.rangeClosed(1, 25)
                .mapToObj(i -> new Aviso(destino, null, TipoAviso.ACEPTADA,
                        "Centro prueba " + i, fecha)).toList());
    }
}
