package com.ecosolicitud;

import com.ecosolicitud.demo.DemoService;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.solicitud.SolicitudService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NombreSolicitudTest {

    @LocalServerPort
    private int port;

    @Autowired
    private DemoService demo;

    @Autowired
    private SolicitudService solicitudes;

    private HtmlUnitDriver driver;

    @BeforeEach
    void setUp() {
        demo.reiniciar();
        driver = new HtmlUnitDriver();
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ana Prueba", "María Martina López", "Martina López"})
    @DisplayName("El nombre reemplazado se guarda y muestra sin concatenar el precargado")
    void nombreExacto(String nombre) {
        String base = "http://localhost:" + port;
        driver.get(base + "/nueva?ciudad=RIVERA&organizacionId=acopio-verde");
        var campo = driver.findElement(By.name("nombre"));
        assertThat(campo.getAttribute("value")).isEqualTo("Martina López");
        campo.clear();
        campo.sendKeys(nombre);
        assertThat(campo.getAttribute("value")).isEqualTo(nombre);
        driver.findElement(By.cssSelector("input[name='materiales'][value='CARTON']")).click();
        driver.findElement(By.name("contacto")).sendKeys("099 111 222");
        driver.findElement(By.name("direccion")).sendKeys("Dirección de prueba 123");
        driver.findElement(By.cssSelector(".form-nueva button[type='submit']")).click();
        assertThat(driver.getCurrentUrl()).endsWith(Rutas.MIS_SOLICITUDES);

        var creada = solicitudes.todas().getLast();
        assertThat(creada.nombreCiudadano()).isEqualTo(nombre);

        driver.findElement(By.cssSelector(".sidebar .rol-switch button[value='ORGANIZACION']"))
                .click();
        assertThat(driver.getCurrentUrl()).endsWith(Rutas.ORG_SOLICITUDES);
        var tarjeta = driver.findElements(By.cssSelector("article.tarjeta h3")).stream()
                .filter(e -> e.getText().startsWith("#" + creada.id() + " · "))
                .findFirst().orElseThrow();
        assertThat(tarjeta.getText()).isEqualTo("#" + creada.id() + " · " + nombre);
    }
}
