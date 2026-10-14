package com.ecosolicitud;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DesignTest {

    private static final String[] SECCIONES = {
            "id=\"ds-encabezado\"", "id=\"ds-ciudad\"", "id=\"ds-grupos\"",
            "id=\"ds-banner\"", "id=\"ds-ecosistema\""};

    @Autowired
    private TestRestTemplate rest;

    @Test
    void designRespondeConLasCincoSecciones() {
        var r = rest.getForEntity("/design", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains(SECCIONES);
    }

    @Test
    void designCssSeCargaUnaVezDespuesDeAppCss() {
        var hoja = "/css/" + "design" + ".css";
        for (String ruta : new String[]{"/", "/design", "/guia"}) {
            var cuerpo = rest.getForEntity(ruta, String.class).getBody();
            assertThat(cuerpo).containsOnlyOnce(hoja);
            assertThat(cuerpo.indexOf("/css/app.css")).isLessThan(cuerpo.indexOf(hoja));
        }
    }
}
