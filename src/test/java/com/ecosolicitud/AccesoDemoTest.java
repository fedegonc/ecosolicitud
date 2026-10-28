package com.ecosolicitud;

import org.openqa.selenium.By;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

// Los ensayos de organizacion entran por el formulario real: no hay bypass
// de seguridad por perfil ni sesiones autenticadas automaticamente.
final class AccesoDemoTest {

    private AccesoDemoTest() {
    }

    static void entrar(HtmlUnitDriver driver, String base) {
        driver.get(base + "/modo/dev");
        driver.findElement(By.name("username")).sendKeys("admin-test");
        driver.findElement(By.name("password")).sendKeys("clave-solo-de-pruebas");
        driver.findElement(By.cssSelector("main form button[type='submit']")).click();
        assertThat(driver.findElements(By.cssSelector(".rol-switch"))).isNotEmpty();
    }

    static Acceso entrar(TestRestTemplate rest) {
        var driver = new HtmlUnitDriver();
        try {
            entrar(driver, rest.getRootUri());
            return new Acceso(driver.manage().getCookieNamed("JSESSIONID").getValue(),
                    driver.findElement(By.cssSelector("input[name='_csrf']")).getAttribute("value"));
        } finally {
            driver.quit();
        }
    }

    record Acceso(String sesion, String csrf) {
        String cookie() {
            return "JSESSIONID=" + sesion;
        }
    }
}
