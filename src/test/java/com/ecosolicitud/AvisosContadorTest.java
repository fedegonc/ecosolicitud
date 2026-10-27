package com.ecosolicitud;

import java.util.List;

import com.ecosolicitud.demo.DemoService;
import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Actor;
import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.solicitud.AvisoService;
import com.ecosolicitud.solicitud.Estado;
import com.ecosolicitud.solicitud.Resultado;
import com.ecosolicitud.solicitud.SolicitudService;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

// El contador de avisos no se consulta periódicamente: viene en cada página y
// un único elemento lo pide al volver a la pestaña; la respuesta fuera de
// banda actualiza los dos badges: barra lateral y menú móvil. Sin JavaScript: el Rhino de HtmlUnit no
// interpreta htmx 2; que htmx efectivamente pida y reemplace se verificó con
// Chromium (Playwright). Acá se fija la estructura de la que depende.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Epic("Rendimiento")
@Feature("Contador de avisos")
class AvisosContadorTest {

    private static final String CIUDADANO_DEMO = "ciudadano-demo";

    @LocalServerPort
    private int port;

    @Autowired
    private DemoService demo;

    @Autowired
    private SolicitudService solicitudes;

    @Autowired
    private OrganizacionService organizaciones;

    @Autowired
    private AvisoService avisos;

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

    @Test
    @DisplayName("Sin consultas periódicas: un pedido al volver a la pestaña actualiza los dos badges")
    void unPedidoActualizaLosDosBadges() {
        driver.get("http://localhost:" + port + Rutas.MIS_SOLICITUDES);
        var pollers = driver.findElements(By.cssSelector("[hx-get='" + Rutas.AVISOS_CONTADOR + "']"));
        assertThat(pollers).hasSize(1);
        assertThat(pollers.getFirst().getDomAttribute("hx-trigger"))
                .isEqualTo("visibilitychange[document.visibilityState === 'visible'] from:document");
        assertThat(badges()).hasSize(2);

        var ciudadano = new Actor(Rol.CIUDADANO, CIUDADANO_DEMO, "Martina López", null);
        long antes = avisos.avisosSinLeer(ciudadano);
        completarLaEnCursoDeMartina();
        long esperado = antes + 1;
        assertThat(avisos.avisosSinLeer(ciudadano)).isEqualTo(esperado);

        assertThat(pollers.getFirst().getDomAttribute("hx-swap")).isEqualTo("none");

        // lo que recibe htmx: un fragmento fuera de banda dirigido a todos los .badge-live
        driver.get("http://localhost:" + port + Rutas.AVISOS_CONTADOR);
        var respuesta = driver.getPageSource();
        assertThat(respuesta).contains("hx-swap-oob=\"innerHTML:.badge-live\"");
        assertThat(respuesta).contains("data-n=\"" + esperado + "\"");
    }

    private List<WebElement> badges() {
        return driver.findElements(By.cssSelector(".badge-live"));
    }

    private void completarLaEnCursoDeMartina() {
        var enCurso = solicitudes.todas().stream()
                .filter(s -> s.estado() == Estado.EN_CURSO && s.nombreCiudadano().equals("Martina López"))
                .findFirst().orElseThrow();
        var organizacionId = organizaciones.todas().stream()
                .filter(o -> o.nombre().equals(enCurso.organizacionNombre()))
                .map(OrganizacionInfo::id).findFirst().orElseThrow();
        var org = new Actor(Rol.ORGANIZACION, null, null, organizacionId);
        assertThat(solicitudes.completar(org, enCurso.id(), enCurso.version())).isEqualTo(Resultado.OK);
    }
}
