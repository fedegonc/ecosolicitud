package com.ecosolicitud;

import com.ecosolicitud.shared.Rutas;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.shared.Seccion;

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
@Epic("Contratos")
@Feature("Obligaciones de prueba sobre el modelo finito")
class ContratosTest {

    private static final Map<Seccion, String> TITULOS = Map.of(
            Seccion.NUEVA, "Nueva solicitud",
            Seccion.MIS_SOLICITUDES, "Mis solicitudes",
            Seccion.ACOPIOS, "Centros de acopio",
            Seccion.ORG_SOLICITUDES, "Solicitudes recibidas",
            Seccion.ORG_PERFIL, "Perfil del acopio",
            Seccion.GUIA, "Guía de reciclaje",
            Seccion.COMUNIDAD, "Comunidad",
            Seccion.ESTADISTICAS, "Estadísticas del servicio",
            Seccion.OPINION, "Dejanos tu opinión",
            Seccion.AVISOS, "Avisos");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    private HtmlUnitDriver driver;
    private String base;

    @BeforeEach
    void setUp() {
        driver = new HtmlUnitDriver();
        base = "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }

    @Test
    @DisplayName("Invariantes del sistema, verificadas por enumeración exhaustiva")
    void invariantes() {
        Allure.step("INV-MOD-01 las rutas de Seccion son únicas", () ->
                assertThat(Arrays.stream(Seccion.rutas()).distinct().count())
                        .isEqualTo(Seccion.rutas().length));

        Allure.step("INV-MOD-02 todo rol tiene inicio permitido", () -> {
            for (Rol r : Rol.values()) {
                assertThat(Seccion.paraRol(r)).as(r.name()).isNotEmpty();
                assertThat(Seccion.paraRol(r).stream().map(Seccion::getRuta))
                        .as(r.name()).contains(Seccion.inicioDe(r));
            }
        });

        Allure.step("INV-MOD-03 toda sección pertenece a al menos un rol", () -> {
            for (Seccion s : Seccion.values()) {
                assertThat(Arrays.stream(Rol.values()).anyMatch(s::permite)).as(s.name()).isTrue();
            }
        });

        driver.get(base + Rutas.MIS_SOLICITUDES);

        Allure.step("INV-DOM-01 rol activo con aria-pressed=true, el otro false", () -> {
            var botones = driver.findElements(By.cssSelector(".sidebar .rol-switch button"));
            assertThat(botones).hasSize(2);
            assertThat(botones.get(0).getAttribute("aria-pressed")).isEqualTo("true");
            assertThat(botones.get(0).getAttribute("value")).isEqualTo("CIUDADANO");
            assertThat(botones.get(1).getAttribute("aria-pressed")).isEqualTo("false");
        });

        matriz(Rol.CIUDADANO);

        driver.findElement(By.cssSelector(".sidebar .rol-switch button[value='ORGANIZACION']"))
                .click();
        Allure.step("INV-ROL-01 POST /demo/rol redirige al inicio del rol elegido", () ->
                assertThat(driver.getCurrentUrl()).endsWith(Rutas.ORG_SOLICITUDES));
        matriz(Rol.ORGANIZACION);

        Allure.step("INV-RAIZ-01 / muestra la portada con acceso del rol activo", () -> {
            driver.get(base + Rutas.RAIZ);
            assertThat(driver.getCurrentUrl()).endsWith(Rutas.RAIZ);
            assertThat(driver.findElement(By.cssSelector(".acciones-portada a"))
                    .getAttribute("href")).endsWith(Rutas.ORG_SOLICITUDES);
            driver.findElement(By.cssSelector(".sidebar .rol-switch button[value='CIUDADANO']"))
                    .click();
            driver.get(base + Rutas.RAIZ);
            assertThat(driver.findElement(By.cssSelector(".acciones-portada a"))
                    .getAttribute("href")).endsWith(Rutas.NUEVA);
        });

        Allure.step("INV-SES-01 dos sesiones mantienen roles independientes", () -> {
            var otra = new HtmlUnitDriver();
            try {
                otra.get(base + Rutas.MIS_SOLICITUDES);
                assertThat(enlacesSidebar(otra)).isEqualTo(
                        Seccion.paraRol(Rol.CIUDADANO).stream().map(Seccion::getRuta).toList());
            } finally {
                otra.quit();
            }
            assertThat(enlacesSidebar()).isEqualTo(
                    Seccion.paraRol(Rol.CIUDADANO).stream().map(Seccion::getRuta).toList());
        });

        Allure.step("INV-POST-01 POST sin token CSRF responde 403", () -> {
            var body = new LinkedMultiValueMap<String, String>();
            body.add("rol", "CIUDADANO");
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            var respuesta = rest.postForEntity(Rutas.DEMO_ROL,
                    new HttpEntity<>(body, headers), String.class);
            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        });

        Allure.step("INV-POST-02 POST con rol inválido responde 400", () -> {
            var pagina = rest.getForEntity(Rutas.MIS_SOLICITUDES, String.class);
            var matcher = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"")
                    .matcher(pagina.getBody());
            assertThat(matcher.find()).isTrue();
            var body = new LinkedMultiValueMap<String, String>();
            body.add("rol", "REY_MAGO");
            body.add("_csrf", matcher.group(1));
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.add(HttpHeaders.COOKIE, pagina.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
            var respuesta = rest.postForEntity(Rutas.DEMO_ROL,
                    new HttpEntity<>(body, headers), String.class);
            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        });

        Allure.step("INV-HTTP-01 una ruta fuera del enum responde 404", () ->
                assertThat(rest.getForEntity("/no-existe", String.class).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    private void matriz(Rol rol) {
        Allure.step("INV-NAV-01 sidebar ≡ Seccion.paraRol(" + rol + ")", () ->
                assertThat(enlacesSidebar()).isEqualTo(
                        Seccion.paraRol(rol).stream().map(Seccion::getRuta).toList()));
        for (Seccion s : Seccion.values()) {
            if (s.permite(rol)) {
                Allure.step("GET " + s.getRuta() + " [" + rol + "] renderiza correcta", () -> {
                    driver.get(base + s.getRuta());
                    assertThat(driver.getCurrentUrl()).endsWith(s.getRuta());
                    assertThat(driver.findElement(By.tagName("h1")).getText())
                            .isEqualTo(TITULOS.get(s));
                    for (String nav : new String[]{".sidebar", ".menu-movil"}) {
                        var activos = driver.findElements(
                                By.cssSelector(nav + " a[aria-current='page']"));
                        assertThat(activos).as(nav).hasSize(1);
                        assertThat(activos.get(0).getAttribute("href")).endsWith(s.getRuta());
                    }
                    assertThat(driver.findElement(By.cssSelector(".demo")).getText())
                            .isEqualTo("Modo demostración");
                    assertThat(driver.findElements(By.cssSelector("input[name='_csrf']")))
                            .isNotEmpty();
                });
            } else {
                String inicio = Seccion.inicioDe(rol);
                Allure.step("GET " + s.getRuta() + " [" + rol + "] → redirect " + inicio, () -> {
                    driver.get(base + s.getRuta());
                    assertThat(driver.getCurrentUrl()).endsWith(inicio);
                });
            }
        }
    }

    private List<String> enlacesSidebar() {
        return enlacesSidebar(driver);
    }

    private List<String> enlacesSidebar(HtmlUnitDriver d) {
        return d.findElements(By.cssSelector(".sidebar .secciones a")).stream()
                .map(a -> a.getAttribute("href").substring(base.length()))
                .toList();
    }
}
