package com.ecosolicitud;

import com.ecosolicitud.shared.Rutas;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import com.ecosolicitud.demo.DemoService;
import com.ecosolicitud.solicitud.Estado;
import com.ecosolicitud.solicitud.SolicitudInfo;
import com.ecosolicitud.solicitud.SolicitudService;

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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Epic("Etapa 5: máquina de estados")
@Feature("Aceptar, rechazar, completar y cancelar solicitudes")
class SolicitudE5Test {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private SolicitudService servicio;

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
    @DisplayName("Organización: aceptar, rechazar, completar y errores RN-01/RN-02/RN-07")
    void accionesOrganizacion() {
        var org = sesion();
        org.post(Rutas.DEMO_ROL, b -> b.add("rol", "ORGANIZACION"));
        org.post(Rutas.DEMO_ORGANIZACION, b -> b.add("id", "frontera-limpia"));

        var pendiente = buscar(Estado.PENDIENTE, "Frontera");
        var enCurso = buscar(Estado.EN_CURSO, "Frontera");

        Allure.step("Acepta una pendiente → EN_CURSO con mensaje", () -> {
            var r = org.post("/org/solicitudes/" + pendiente.id() + "/aceptar",
                    b -> b.add("version", String.valueOf(pendiente.version())));
            assertThat(r.getBody()).contains("aceptada");
            assertThat(buscar(pendiente.id()).estado()).isEqualTo(Estado.EN_CURSO);
        });

        Allure.step("Completa una en curso → COMPLETADA con finalizadaEn", () -> {
            var r = org.post("/org/solicitudes/" + enCurso.id() + "/completar",
                    b -> b.add("version", String.valueOf(enCurso.version())));
            assertThat(r.getBody()).contains("completada");
            var s = buscar(enCurso.id());
            assertThat(s.estado()).isEqualTo(Estado.COMPLETADA);
            assertThat(s.finalizadaEn()).isNotNull();
        });

        Allure.step("Rechaza una en curso → RECHAZADA con finalizadaEn (Q-02)", () -> {
            var s = buscar(pendiente.id());
            var r = org.post("/org/solicitudes/" + s.id() + "/rechazar",
                    b -> b.add("version", String.valueOf(s.version())));
            assertThat(r.getBody()).contains("rechazada");
            var despues = buscar(pendiente.id());
            assertThat(despues.estado()).isEqualTo(Estado.RECHAZADA);
            assertThat(despues.finalizadaEn()).isNotNull();
        });

        Allure.step("Versión desactualizada → conflicto sin cambios (FE-02)", () -> {
            var s = buscar(Estado.PENDIENTE, "Frontera");
            var r = org.post("/org/solicitudes/" + s.id() + "/aceptar",
                    b -> b.add("version", "99"));
            assertThat(r.getBody()).contains("cambió antes");
            assertThat(buscar(s.id()).estado()).isEqualTo(Estado.PENDIENTE);
        });

        Allure.step("Transición inválida → mensaje y estado intacto (FE-03)", () -> {
            var s = buscar(Estado.PENDIENTE, "Frontera");
            var r = org.post("/org/solicitudes/" + s.id() + "/completar",
                    b -> b.add("version", String.valueOf(s.version())));
            assertThat(r.getBody()).contains("no admite");
            assertThat(buscar(s.id()).estado()).isEqualTo(Estado.PENDIENTE);
        });

        Allure.step("Otra organización intenta aceptar → 403 sin cambios", () -> {
            org.post(Rutas.DEMO_ORGANIZACION, b -> b.add("id", "coleta-solidaria"));
            var s = buscar(Estado.PENDIENTE, "Frontera");
            var r = org.post("/org/solicitudes/" + s.id() + "/aceptar",
                    b -> b.add("version", String.valueOf(s.version())));
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(buscar(s.id()).estado()).isEqualTo(Estado.PENDIENTE);
        });

        Allure.step("Acción sobre id inexistente → 404", () -> {
            var r = org.post("/org/solicitudes/99999/aceptar",
                    b -> b.add("version", "0"));
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        });
    }

    @Test
    @DisplayName("Ciudadano: cancelar propia pendiente y restricciones RN-02")
    void accionesCiudadano() {
        var ciudadano = sesion();

        var propia = new long[1];
        Allure.step("Crea una solicitud nueva queda PENDIENTE propia", () -> {
            ciudadano.post(Rutas.NUEVA, b -> {
                b.add("ciudad", "RIVERA");
                b.add("materiales", "PLASTICO");
                b.add("organizacionId", "frontera-limpia");
                b.add("nombre", "Martina López");
                b.add("contacto", "099 000 111");
                b.add("direccion", "Uruguay 100");
            });
            var todas = servicio.todas();
            propia[0] = todas.get(todas.size() - 1).id();
            assertThat(buscar(propia[0]).estado()).isEqualTo(Estado.PENDIENTE);
        });

        Allure.step("Cancela su pendiente → CANCELADA con finalizadaEn", () -> {
            var s = buscar(propia[0]);
            var r = ciudadano.post("/mis-solicitudes/" + s.id() + "/cancelar",
                    b -> b.add("version", String.valueOf(s.version())));
            assertThat(r.getBody()).contains("cancelada");
            var despues = buscar(propia[0]);
            assertThat(despues.estado()).isEqualTo(Estado.CANCELADA);
            assertThat(despues.finalizadaEn()).isNotNull();
        });

        Allure.step("Cancelar una solicitud en curso es rechazado (CA-09)", () -> {
            var s = buscar(Estado.EN_CURSO, "Frontera");
            var r = ciudadano.post("/mis-solicitudes/" + s.id() + "/cancelar",
                    b -> b.add("version", String.valueOf(s.version())));
            assertThat(r.getBody()).contains("no admite");
            assertThat(buscar(s.id()).estado()).isEqualTo(Estado.EN_CURSO);
        });

        Allure.step("Cancelar la de otro ciudadano → 403", () -> {
            var ajena = servicio.todas().stream()
                    .filter(s -> s.estado() == Estado.PENDIENTE)
                    .findFirst().orElseThrow();
            var r = ciudadano.post("/mis-solicitudes/" + ajena.id() + "/cancelar",
                    b -> b.add("version", String.valueOf(ajena.version())));
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(buscar(ajena.id()).estado()).isEqualTo(Estado.PENDIENTE);
        });

        Allure.step("Cualquier acción sin CSRF → 403", () -> {
            var body = new LinkedMultiValueMap<String, String>();
            body.add("version", "0");
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            var r = rest.postForEntity("/mis-solicitudes/1/cancelar",
                    new HttpEntity<>(body, headers), String.class);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        });
    }

    @Test
    @DisplayName("Bandeja de la organización agrupada y botones por estado")
    void bandeja() {
        Allure.step("Solo las de la organización activa, en 3 grupos", () -> {
            driver.get(base + Rutas.MIS_SOLICITUDES);
            driver.findElement(By.cssSelector(
                    ".sidebar .rol-switch button[value='ORGANIZACION']")).click();
            driver.findElement(By.cssSelector(
                    "#org-select option[value='frontera-limpia']")).click();
            driver.findElement(By.cssSelector(".org-switch button[type='submit']")).click();
            driver.get(base + Rutas.ORG_SOLICITUDES);
            assertThat(driver.findElements(By.cssSelector("h2")).stream()
                    .map(e -> e.getText()).toList())
                    .containsExactly("Pendientes", "En curso", "Cerradas");
            var secciones = driver.findElements(By.cssSelector("main section"));
            var pendientes = secciones.get(0).findElements(By.cssSelector(".tarjeta"));
            assertThat(pendientes).hasSize(2);
            assertThat(pendientes.get(0).getText()).contains("Bruno Pérez");
            assertThat(pendientes.get(1).getText()).contains("Ana Rodríguez");
            assertThat(secciones.get(1).findElements(By.cssSelector(".tarjeta"))).hasSize(1);
            var cerradas = secciones.get(2).findElements(By.cssSelector(".tarjeta"));
            assertThat(cerradas).hasSize(1);
            assertThat(cerradas.get(0).getText()).contains("Completada");
            assertThat(driver.getPageSource()).doesNotContain("Carla Santos");
        });

        Allure.step("Los botones corresponden al estado de cada solicitud", () -> {
            var secciones = driver.findElements(By.cssSelector("main section"));
            var pendiente = secciones.get(0).findElement(By.cssSelector(".tarjeta"));
            assertThat(pendiente.findElements(By.cssSelector("form[action$='/aceptar']")))
                    .hasSize(1);
            assertThat(pendiente.findElements(By.cssSelector("form[action$='/rechazar']")))
                    .hasSize(1);
            var enCurso = secciones.get(1).findElement(By.cssSelector(".tarjeta"));
            assertThat(enCurso.findElements(By.cssSelector("form[action$='/completar']")))
                    .hasSize(1);
            assertThat(enCurso.findElements(By.cssSelector("form[action$='/rechazar']")))
                    .hasSize(1);
            var cerrada = secciones.get(2).findElement(By.cssSelector(".tarjeta"));
            assertThat(cerrada.findElements(By.cssSelector("form"))).isEmpty();
        });
    }

    @Test
    @DisplayName("Cancelar aparece solo en pendientes del ciudadano")
    void botonCancelar() {
        var ciudadano = sesion();
        Allure.step("Crea una pendiente propia", () ->
                ciudadano.post(Rutas.NUEVA, b -> {
                    b.add("ciudad", "RIVERA");
                    b.add("materiales", "PLASTICO");
                    b.add("organizacionId", "frontera-limpia");
                b.add("nombre", "Martina López");
                b.add("contacto", "099 000 111");
                    b.add("direccion", "Uruguay 100");
                }));
        Allure.step("Mis solicitudes muestra Cancelar solo en la pendiente", () -> {
            driver.get(base + Rutas.MIS_SOLICITUDES);
            var fichas = driver.findElements(By.cssSelector("article.tarjeta"));
            var conCancelar = fichas.stream().filter(f ->
                    !f.findElements(By.cssSelector("form[action$='/cancelar']")).isEmpty())
                    .toList();
            assertThat(conCancelar).hasSize(1);
            assertThat(conCancelar.get(0).getText()).contains("Pendiente");
        });
    }

    private SolicitudInfo buscar(Estado estado, String organizacion) {
        return servicio.todas().stream()
                .filter(s -> s.estado() == estado
                        && s.organizacionNombre().contains(organizacion))
                .findFirst().orElseThrow();
    }

    private SolicitudInfo buscar(long id) {
        return servicio.todas().stream().filter(s -> s.id() == id)
                .findFirst().orElseThrow();
    }

    private Sesion sesion() {
        return new Sesion();
    }

    private class Sesion {
        private final String cookie;
        private final String csrf;

        Sesion() {
            var pagina = rest.getForEntity(Rutas.MIS_SOLICITUDES, String.class);
            cookie = pagina.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
            var m = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"")
                    .matcher(pagina.getBody());
            assertThat(m.find()).isTrue();
            csrf = m.group(1);
        }

        ResponseEntity<String> post(String ruta,
                Consumer<MultiValueMap<String, String>> fill) {
            var body = new LinkedMultiValueMap<String, String>();
            fill.accept(body);
            body.add("_csrf", csrf);
            var h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            h.add(HttpHeaders.COOKIE, cookie);
            return rest.postForEntity(ruta, new HttpEntity<>(body, h), String.class);
        }
    }
}
