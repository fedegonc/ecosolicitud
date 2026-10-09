package com.ecosolicitud;

import com.ecosolicitud.shared.Rutas;
import java.util.List;
import java.util.regex.Pattern;

import com.ecosolicitud.demo.DemoService;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.solicitud.Estado;
import com.ecosolicitud.solicitud.Filtro;
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
import org.openqa.selenium.support.ui.Select;
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
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Epic("Etapa 4: crear y listar solicitudes")
@Feature("Nueva solicitud en dos pasos y Mis solicitudes")
class SolicitudE4Test {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private SolicitudService servicio;

    @Autowired
    private OrganizacionService organizaciones;

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
    @DisplayName("Selección en cascada: ciudad → centro → materiales del centro")
    void cascada() {
        Allure.step("Sin ciudad no hay centros, materiales ni dirección", () -> {
            driver.get(base + Rutas.NUEVA);
            assertThat(driver.findElements(By.cssSelector(".opcion-centro"))).isEmpty();
            assertThat(driver.findElements(By.cssSelector("input[name='materiales']"))).isEmpty();
            assertThat(driver.findElements(By.cssSelector("input[name='direccion']"))).isEmpty();
            assertThat(driver.findElements(By.cssSelector("input[name='nombre']"))).isEmpty();
            assertThat(driver.findElements(By.cssSelector("input[name='contacto']"))).isEmpty();
        });

        Allure.step("La ciudad lista solo sus centros", () -> {
            driver.get(base + "/nueva?ciudad=RIVERA");
            var textos = driver.findElements(By.cssSelector(".opcion-centro")).stream()
                    .map(e -> e.getText()).toList();
            assertThat(textos).hasSize(2);
            assertThat(textos).anyMatch(t -> t.contains("Cooperativa Frontera Limpia"));
            assertThat(textos).anyMatch(t -> t.contains("Acopio Verde Rivera"));
            assertThat(driver.getPageSource()).doesNotContain("Coleta Solid");
            assertThat(driver.findElements(By.cssSelector("input[name='materiales']"))).isEmpty();
        });

        Allure.step("El centro elegido define los materiales posibles (FE-04)", () -> {
            driver.get(base + "/nueva?ciudad=RIVERA&organizacionId=frontera-limpia");
            assertThat(valores("materiales")).containsExactly("PLASTICO", "CARTON", "PAPEL", "METAL");
            assertThat(driver.getPageSource()).doesNotContain("Electrónicos");
        });

        Allure.step("Cambiar de centro cambia los materiales", () -> {
            driver.get(base + "/nueva?ciudad=RIVERA&organizacionId=acopio-verde");
            assertThat(valores("materiales")).containsExactly("VIDRIO", "PLASTICO", "CARTON");
        });

        Allure.step("Un material que el centro no recibe se poda del estado", () -> {
            driver.get(base + "/nueva?ciudad=RIVERA&organizacionId=frontera-limpia&materiales=VIDRIO");
            assertThat(driver.findElements(By.cssSelector("input[name='materiales']:checked"))).isEmpty();
        });

        Allure.step("Un centro de otra ciudad no queda seleccionado", () -> {
            driver.get(base + "/nueva?ciudad=RIVERA&organizacionId=coleta-solidaria");
            assertThat(driver.findElements(By.cssSelector("input[name='organizacionId']:checked")))
                    .isEmpty();
        });

        Allure.step("Sin JavaScript el botón Continuar avanza el paso (RF-09)", () -> {
            new Select(driver.findElement(By.name("ciudad"))).selectByValue("RIVERA");
            driver.findElement(By.cssSelector("button[formmethod='get']")).click();
            assertThat(driver.findElements(By.cssSelector(".opcion-centro"))).hasSize(2);
        });

        Allure.step("Pedir retiro desde /acopios fija el centro sin repetir la lista (RF-09)", () -> {
            driver.get(base + Rutas.ACOPIOS);
            driver.findElement(By.linkText("Pedir retiro")).click();
            var fijado = driver.findElements(
                    By.cssSelector("input[name='organizacionId'][type='hidden']"));
            assertThat(fijado).hasSize(1);
            assertThat(fijado.get(0).getAttribute("value")).isEqualTo("acopio-verde");
            assertThat(driver.findElements(By.cssSelector(".opcion-centro"))).isEmpty();
            assertThat(driver.findElements(By.cssSelector("input[name='materiales']"))).isNotEmpty();
            driver.findElement(By.linkText("Cambiar")).click();
            assertThat(driver.findElements(By.cssSelector(".opcion-centro"))).hasSize(2);
        });

        Allure.step("El contacto va antes que la ubicación, con el nombre precargado", () -> {
            driver.get(base + "/nueva?ciudad=RIVERA&organizacionId=frontera-limpia");
            var nombre = driver.findElement(By.cssSelector("input[name='nombre']"));
            assertThat(nombre.getAttribute("value")).isEqualTo("Martina López");
            assertThat(driver.findElements(By.cssSelector("input[name='contacto']")))
                    .hasSize(1);
            var orden = driver.findElements(By.cssSelector("#flujo input[name]")).stream()
                    .map(e -> e.getAttribute("name")).toList();
            assertThat(orden.indexOf("nombre")).isLessThan(orden.indexOf("direccion"));
            assertThat(orden.indexOf("contacto")).isLessThan(orden.indexOf("direccion"));
        });

        Allure.step("Ciudad sin centros lo informa", () -> {
            organizaciones.reemplazarTodas(List.of(
                    organizaciones.buscar("coleta-solidaria").orElseThrow()));
            driver.get(base + "/nueva?ciudad=RIVERA");
            assertThat(driver.findElement(By.cssSelector(".alerta")).getText())
                    .contains("No hay centros de acopio");
        });

        Allure.step("Parámetros inexistentes responden 400", () -> {
            assertThat(rest.getForEntity("/nueva?ciudad=NARNIA", String.class).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(rest.getForEntity("/nueva?materiales=NARNIA", String.class).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        });
    }

    private List<String> valores(String campo) {
        return driver.findElements(By.cssSelector("input[name='" + campo + "']")).stream()
                .map(e -> e.getAttribute("value")).toList();
    }

    @Test
    @DisplayName("POST /nueva: creación válida, validaciones y revalidación RN-04")
    void crear() {
        Allure.step("Crear válida persiste PENDIENTE y redirige con el mensaje", () -> {
            int antes = servicio.todas().size();
            driver.get(base + "/nueva?ciudad=RIVERA&organizacionId=frontera-limpia");
            driver.findElement(By.cssSelector("input[name='materiales'][value='PLASTICO']"))
                    .click();
            driver.findElement(By.cssSelector("input[name='contacto']"))
                    .sendKeys("099 111 000");
            driver.findElement(By.cssSelector("input[name='direccion']"))
                    .sendKeys("Uruguay 1000");
            driver.findElement(By.cssSelector("main form[method='post'] button[type='submit']")).click();
            assertThat(driver.getCurrentUrl()).endsWith(Rutas.MIS_SOLICITUDES);
            assertThat(driver.findElement(By.cssSelector(".aviso")).getText())
                    .contains("enviada a Cooperativa Frontera Limpia");
            var creada = servicio.todas().get(servicio.todas().size() - 1);
            assertThat(servicio.todas()).hasSize(antes + 1);
            assertThat(creada.estado()).isEqualTo(Estado.PENDIENTE);
            assertThat(creada.creadaEn()).isNotNull();
            assertThat(creada.finalizadaEn()).isNull();
        });

        Allure.step("Sin dirección no guarda y muestra el error con los valores (FE-01)", () -> {
            int antes = servicio.todas().size();
            driver.get(base + "/nueva?ciudad=RIVERA&organizacionId=frontera-limpia");
            driver.findElement(By.cssSelector("input[name='materiales'][value='PLASTICO']"))
                    .click();
            driver.findElement(By.cssSelector("input[name='contacto']")).sendKeys("099 111 000");
            driver.findElement(By.cssSelector("main form[method='post'] button[type='submit']")).click();
            assertThat(driver.findElements(By.cssSelector(".error"))).isNotEmpty();
            assertThat(servicio.todas()).hasSize(antes);
        });

        Allure.step("Sin materiales no guarda y muestra el error por campo", () -> {
            int antes = servicio.todas().size();
            var respuesta = postConCsrf(Rutas.NUEVA, body -> {
                body.add("ciudad", "RIVERA");
                body.add("organizacionId", "frontera-limpia");
                body.add("nombre", "N");
                body.add("contacto", "T");
                body.add("direccion", "Calle 1");
            });
            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(respuesta.getBody()).contains("al menos un material");
            assertThat(servicio.todas()).hasSize(antes);
        });

        Allure.step("Dirección de 121 caracteres es rechazada", () -> {
            int antes = servicio.todas().size();
            var respuesta = postConCsrf(Rutas.NUEVA, body -> {
                body.add("ciudad", "RIVERA");
                body.add("materiales", "PLASTICO");
                body.add("organizacionId", "frontera-limpia");
                body.add("nombre", "N");
                body.add("contacto", "T");
                body.add("direccion", "x".repeat(121));
            });
            assertThat(respuesta.getBody()).contains("120");
            assertThat(servicio.todas()).hasSize(antes);
        });

        Allure.step("Organización incompatible enviada a mano no guarda (RN-04)", () -> {
            int antes = servicio.todas().size();
            var respuesta = postConCsrf(Rutas.NUEVA, body -> {
                body.add("ciudad", "RIVERA");
                body.add("materiales", "VIDRIO");
                body.add("organizacionId", "frontera-limpia");
                body.add("nombre", "N");
                body.add("contacto", "T");
                body.add("direccion", "Calle 1");
            });
            assertThat(respuesta.getBody()).contains("no lo recibe el centro elegido");
            assertThat(servicio.todas()).hasSize(antes);
        });

        Allure.step("Centro de otra ciudad no guarda (RN-04)", () -> {
            int antes = servicio.todas().size();
            var respuesta = postConCsrf(Rutas.NUEVA, body -> {
                body.add("ciudad", "RIVERA");
                body.add("materiales", "CARTON");
                body.add("organizacionId", "coleta-solidaria");
                body.add("nombre", "N");
                body.add("contacto", "T");
                body.add("direccion", "Calle 1");
            });
            assertThat(respuesta.getBody()).contains("Elegí un centro de acopio de la ciudad");
            assertThat(servicio.todas()).hasSize(antes);
        });

        Allure.step("POST /nueva sin CSRF responde 403", () -> {
            var body = new LinkedMultiValueMap<String, String>();
            body.add("ciudad", "RIVERA");
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            var respuesta = rest.postForEntity(Rutas.NUEVA,
                    new HttpEntity<>(body, headers), String.class);
            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        });
    }

    @Test
    @DisplayName("Mis solicitudes: propias, orden, filtros y fechas")
    void lista() {
        Allure.step("Muestra solo las del ciudadano demo, más nuevas primero", () -> {
            driver.get(base + Rutas.MIS_SOLICITUDES);
            var fichas = driver.findElements(By.cssSelector(".tarjeta"));
            assertThat(fichas).hasSize(2);
            assertThat(fichas.get(0).getText()).contains("En curso");
            assertThat(fichas.get(1).getText()).contains("Completada");
            assertThat(driver.getPageSource()).doesNotContain("Sarandí 56");
        });

        Allure.step("Filtros abiertas y cerradas muestran los estados definidos", () -> {
            driver.get(base + Rutas.MIS_SOLICITUDES + "?filtro=abiertas");
            assertThat(driver.findElements(By.cssSelector(".tarjeta"))).hasSize(1);
            assertThat(driver.findElement(By.cssSelector(".tarjeta")).getText())
                    .contains("En curso");
            driver.get(base + Rutas.MIS_SOLICITUDES + "?filtro=cerradas");
            var cerrada = driver.findElements(By.cssSelector(".tarjeta"));
            assertThat(cerrada).hasSize(1);
            assertThat(cerrada.get(0).getText()).contains("Completada");
            driver.get(base + Rutas.MIS_SOLICITUDES + "?filtro=inventado");
            assertThat(driver.findElements(By.cssSelector(".tarjeta"))).hasSize(2);
        });

        Allure.step("Una final muestra fecha de finalización; una abierta no", () -> {
            driver.get(base + Rutas.MIS_SOLICITUDES);
            var fichas = driver.findElements(By.cssSelector(".tarjeta"));
            assertThat(fichas.get(0).getText()).doesNotContain("Finalizada el");
            assertThat(fichas.get(1).getText()).contains("Finalizada el");
        });
    }

    @Test
    @DisplayName("Dataset: I1, RN-12 y reinicio restaura ambos datasets")
    void dataset() {
        Allure.step("Todas las solicitudes del dataset cumplen I1", () ->
                assertThat(servicio.todas()).allSatisfy(s ->
                        assertThat(s.finalizadaEn() != null).isEqualTo(s.estado().esFinal())));

        Allure.step("Quitar un material del perfil no altera solicitudes existentes (RN-12)", () -> {
            var org = organizaciones.buscar("frontera-limpia").orElseThrow();
            organizaciones.actualizarPerfil("frontera-limpia",
                    List.of(mat("CARTON"), mat("PAPEL")), org.horario(), org.telefono(),
                    org.ubicacion(), org.version());
            var propias = servicio.misSolicitudes("ciudadano-demo", Filtro.TODAS);
            assertThat(propias).anySatisfy(s ->
                    assertThat(s.materiales()).containsExactly(mat("PLASTICO")));
        });

        Allure.step("Reiniciar restaura organizaciones y solicitudes", () -> {
            servicio.reemplazarTodas(List.of());
            organizaciones.reemplazarTodas(List.of(
                    organizaciones.buscar("frontera-limpia").orElseThrow()));
            demo.reiniciar();
            assertThat(servicio.todas()).hasSize(5);
            assertThat(organizaciones.todas()).hasSize(3);
            assertThat(servicio.misSolicitudes("ciudadano-demo", Filtro.TODAS)).hasSize(2);
        });
    }

    private static Material mat(String codigo) {
        return new Material(codigo, codigo, "botella");
    }

    private org.springframework.http.ResponseEntity<String> postConCsrf(String ruta,
            java.util.function.Consumer<MultiValueMap<String, String>> completa) {
        var pagina = rest.getForEntity(Rutas.MIS_SOLICITUDES, String.class);
        var matcher = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"")
                .matcher(pagina.getBody());
        assertThat(matcher.find()).isTrue();
        var body = new LinkedMultiValueMap<String, String>();
        completa.accept(body);
        body.add("_csrf", matcher.group(1));
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add(HttpHeaders.COOKIE, pagina.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
        return rest.postForEntity(ruta, new HttpEntity<>(body, headers), String.class);
    }
}
