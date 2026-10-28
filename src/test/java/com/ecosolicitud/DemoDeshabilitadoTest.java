package com.ecosolicitud;

import com.ecosolicitud.shared.Rutas;
import java.util.regex.Pattern;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ecosolicitud.demo.habilitada=false")
@ActiveProfiles("test")
@Epic("Etapa 3: organizaciones y datos de demo")
@Feature("Modo demo deshabilitado")
class DemoDeshabilitadoTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void interruptorGlobalImpideElIngresoAunqueHayaCredenciales() {
        var driver = new org.openqa.selenium.htmlunit.HtmlUnitDriver();
        try {
            driver.get(rest.getRootUri() + "/modo/dev");
            driver.findElement(org.openqa.selenium.By.name("username")).sendKeys("admin-test");
            driver.findElement(org.openqa.selenium.By.name("password")).sendKeys("clave-solo-de-pruebas");
            driver.findElement(org.openqa.selenium.By.cssSelector("main form button[type='submit']")).click();
            assertThat(driver.getCurrentUrl()).endsWith("/modo/dev?error");
        } finally {
            driver.quit();
        }
    }

    @Test
    @DisplayName("Sin modo demo no hay endpoint de reinicio ni botón")
    void reinicioDeshabilitado() {
        Allure.step("POST /demo/reiniciar con CSRF responde 403", () -> {
            var pagina = rest.getForEntity(Rutas.RAIZ, String.class);
            var matcher = Pattern.compile("name=\"_csrf\"\\s+(?:value|content)=\"([^\"]+)\"")
                    .matcher(pagina.getBody());
            assertThat(matcher.find()).isTrue();
            var body = new LinkedMultiValueMap<String, String>();
            body.add("_csrf", matcher.group(1));
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.add(HttpHeaders.COOKIE, pagina.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
            var respuesta = rest.postForEntity(Rutas.DEMO_REINICIAR,
                    new HttpEntity<>(body, headers), String.class);
            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        });

        Allure.step("El botón de reinicio no aparece en la página", () -> {
            var pagina = rest.getForEntity(Rutas.RAIZ, String.class);
            assertThat(pagina.getBody()).doesNotContain(Rutas.DEMO_REINICIAR);
        });
    }
}
