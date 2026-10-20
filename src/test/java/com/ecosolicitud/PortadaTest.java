package com.ecosolicitud;

import com.ecosolicitud.shared.Rutas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PortadaTest {

    private static final String[] SECCIONES = {
            "id=\"ds-saludo\"", "id=\"ds-titulo\"", "id=\"ds-acciones\"",
            "id=\"ds-ultimo\"", "id=\"ds-consejos\""};

    @Autowired
    private TestRestTemplate rest;

    @Test
    void portadaRespondeConLasCincoSecciones() {
        var r = rest.getForEntity("/?ciudad=livramento", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains(SECCIONES);
    }

    @Test
    void designCssSeCargaUnaVezDespuesDeAppCss() {
        var hoja = "/css/" + "design" + ".css";
        for (String ruta : new String[]{Rutas.RAIZ, Rutas.ACOPIOS, Rutas.GUIA}) {
            var cuerpo = rest.getForEntity(ruta, String.class).getBody();
            assertThat(cuerpo).containsOnlyOnce(hoja);
            assertThat(cuerpo.indexOf("/css/app.css")).isLessThan(cuerpo.indexOf(hoja));
        }
    }
}
