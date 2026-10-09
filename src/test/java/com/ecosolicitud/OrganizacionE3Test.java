package com.ecosolicitud;

import com.ecosolicitud.shared.Rutas;
import java.util.List;
import java.util.regex.Pattern;

import com.ecosolicitud.demo.DemoService;
import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Epic("Etapa 3: organizaciones y datos de demo")
@Feature("Dataset, selector, acopios y perfil de acopio")
class OrganizacionE3Test {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private OrganizacionService servicio;

    @Autowired
    private DemoService demo;

    private HtmlUnitDriver driver;
    private String base;

    @BeforeEach
    void setUp() {
        demo.reiniciar();
        driver = new HtmlUnitDriver();
        base = "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }

    @Test
    @DisplayName("Dataset, selector de organización, acopios, perfil y reinicio")
    void e3() {
        driver.get(base + Rutas.MIS_SOLICITUDES);

        Allure.step("El selector de organización no aparece en modo ciudadano", () ->
                assertThat(driver.findElements(By.cssSelector(".org-switch"))).isEmpty());

        Allure.step("Centros de acopio agrupa por ciudad con horario, teléfono y materiales", () -> {
            driver.get(base + Rutas.ACOPIOS);
            assertThat(driver.findElements(By.cssSelector("h2")).stream()
                    .map(e -> e.getText()).toList())
                    .containsExactly("Rivera", "Sant'Ana do Livramento");
            var fichas = driver.findElements(By.cssSelector(".tarjeta h3"));
            assertThat(fichas.stream().map(e -> e.getText()).toList())
                    .containsExactlyInAnyOrder("Cooperativa Frontera Limpia",
                            "Acopio Verde Rivera", "Coleta Solidária Livramento");
            assertThat(driver.findElements(By.cssSelector(".tarjeta .etiqueta-material")))
                    .isNotEmpty();
            assertThat(driver.getPageSource()).contains("+598");
        });

        Allure.step("En modo organización aparece el selector y por defecto actúa la primera", () -> {
            driver.findElement(By.cssSelector(".sidebar .rol-switch button[value='ORGANIZACION']"))
                    .click();
            var seleccionada = driver.findElements(By.cssSelector("#org-select option"))
                    .stream().filter(e -> e.isSelected()).findFirst().orElseThrow();
            assertThat(seleccionada.getAttribute("value")).isEqualTo("acopio-verde");
        });

        Allure.step("Elegir frontera-limpia hace que el perfil muestre esa organización", () -> {
            driver.get(base + Rutas.ORG_PERFIL);
            driver.findElement(By.cssSelector("#org-select option[value='frontera-limpia']"))
                    .click();
            driver.findElement(By.cssSelector(".org-switch button[type='submit']")).click();
            driver.get(base + Rutas.ORG_PERFIL);
            assertThat(driver.getPageSource()).contains("Cooperativa Frontera Limpia");
            var chequeados = driver.findElements(
                    By.cssSelector("input[name='materiales']:checked")).stream()
                    .map(e -> e.getAttribute("value")).toList();
            assertThat(chequeados).containsExactlyInAnyOrder(
                    "PLASTICO", "CARTON", "PAPEL", "METAL");
        });

        Allure.step("Guardar un perfil válido persiste, redirige y muestra el aviso", () -> {
            var telefono = driver.findElement(By.cssSelector("input[name='telefono']"));
            telefono.clear();
            telefono.sendKeys("+598 99 123 456");
            driver.findElement(By.cssSelector(".ficha button[type='submit']")).click();
            assertThat(driver.getCurrentUrl()).endsWith(Rutas.ORG_PERFIL);
            assertThat(driver.findElement(By.cssSelector(".aviso")).getText())
                    .isEqualTo("Cambios guardados");
            assertThat(servicio.buscar("frontera-limpia").orElseThrow().telefono())
                    .isEqualTo("+598 99 123 456");
        });

        Allure.step("Guardar sin materiales muestra el error y no persiste (RN-06)", () -> {
            driver.get(base + Rutas.ORG_PERFIL);
            driver.findElements(By.cssSelector("input[name='materiales']:checked"))
                    .forEach(e -> e.click());
            driver.findElement(By.cssSelector(".ficha button[type='submit']")).click();
            assertThat(driver.findElements(By.cssSelector("fieldset .error"))).isNotEmpty();
            assertThat(driver.findElement(By.cssSelector("fieldset .error")).getText())
                    .contains("al menos un material");
            assertThat(servicio.buscar("frontera-limpia").orElseThrow().materiales())
                    .containsExactlyInAnyOrder(mat("PLASTICO"), mat("CARTON"),
                            mat("PAPEL"), mat("METAL"));
        });

        Allure.step("Versión desactualizada muestra conflicto y no persiste (RN-07)", () -> {
            driver.get(base + Rutas.ORG_PERFIL);
            String version = driver.findElement(By.cssSelector("input[name='version']"))
                    .getAttribute("value");
            servicio.actualizarPerfil("frontera-limpia",
                    servicio.buscar("frontera-limpia").orElseThrow().materiales(),
                    "Horario concurrente", "+598 92 000 111", null,
                    Long.parseLong(version));
            var telefono = driver.findElement(By.cssSelector("input[name='telefono']"));
            telefono.clear();
            telefono.sendKeys("+598 77 777 777");
            driver.findElement(By.cssSelector(".ficha button[type='submit']")).click();
            assertThat(driver.findElement(By.cssSelector(".alerta")).getText())
                    .contains("cambió mientras editabas");
            assertThat(servicio.buscar("frontera-limpia").orElseThrow().telefono())
                    .isNotEqualTo("+598 77 777 777");
        });

        Allure.step("POST /demo/organizacion con id inexistente responde 400", () -> {
            var respuesta = postConCsrf(Rutas.DEMO_ORGANIZACION, "id", "no-existe");
            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        });

        Allure.step("Id de sesión obsoleto cae a la primera organización del orden", () -> {
            var sinAcopioVerde = servicio.todas().stream()
                    .filter(o -> !o.id().equals("acopio-verde")).toList();
            servicio.reemplazarTodas(sinAcopioVerde);
            driver.get(base + Rutas.ORG_PERFIL);
            assertThat(driver.getPageSource()).contains("Coleta Solidária Livramento");
            demo.reiniciar();
        });

        Allure.step("Reiniciar restaura el dataset y vuelve a la misma página", () -> {
            servicio.actualizarPerfil("acopio-verde", List.of(mat("VIDRIO")),
                    "Alterado", "+598 92 999 888", null,
                    servicio.buscar("acopio-verde").orElseThrow().version());
            driver.get(base + Rutas.ORG_SOLICITUDES);
            driver.findElement(By.cssSelector(".sidebar .reiniciar button")).click();
            assertThat(driver.getCurrentUrl()).endsWith(Rutas.ORG_SOLICITUDES);
            assertThat(servicio.buscar("acopio-verde").orElseThrow().horario())
                    .isEqualTo("Lun a Sáb 9 a 13 h");
            assertThat(servicio.todas()).hasSize(3);
        });

        Allure.step("POST /demo/reiniciar sin CSRF responde 403", () -> {
            var body = new LinkedMultiValueMap<String, String>();
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            var respuesta = rest.postForEntity(Rutas.DEMO_REINICIAR,
                    new HttpEntity<>(body, headers), String.class);
            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        });
    }

    private static Material mat(String codigo) {
        return new Material(codigo, codigo, "botella");
    }

    private org.springframework.http.ResponseEntity<String> postConCsrf(String ruta,
            String campo, String valor) {
        var pagina = rest.getForEntity(Rutas.MIS_SOLICITUDES, String.class);
        var matcher = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"")
                .matcher(pagina.getBody());
        assertThat(matcher.find()).isTrue();
        var body = new LinkedMultiValueMap<String, String>();
        body.add(campo, valor);
        body.add("_csrf", matcher.group(1));
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add(HttpHeaders.COOKIE, pagina.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
        return rest.postForEntity(ruta, new HttpEntity<>(body, headers), String.class);
    }
}
