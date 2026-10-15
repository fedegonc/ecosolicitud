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
            "id=\"portada-lugar\"", "id=\"portada-respuesta\"", "id=\"portada-buscar\"",
            "id=\"portada-preguntas\"",
            "id=\"portada-aviso\"", "id=\"portada-mas\""};

    @Autowired
    private TestRestTemplate rest;

    @Test
    void portadaRespondeConLasCincoSecciones() {
        var r = rest.getForEntity("/?ciudad=livramento", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains(SECCIONES);
    }

    // Rivera informa (la Intendencia recolecta casa por casa); el retiro se pide en Livramento
    @Test
    void soloLivramentoOfrecePedirElRetiro() {
        assertThat(preguntas("/?ciudad=livramento")).contains("href=\"" + Rutas.NUEVA + "\"");
        assertThat(preguntas("/?ciudad=rivera")).doesNotContain("href=\"" + Rutas.NUEVA + "\"");
    }

    @Test
    void sinDatosPublicadosNoHayRespuesta() {
        assertThat(rest.getForEntity("/?ciudad=tranqueras", String.class).getBody())
                .doesNotContain("id=\"portada-respuesta\"")
                .contains("id=\"portada-preguntas\"");
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

    private String preguntas(String url) {
        var cuerpo = rest.getForEntity(url, String.class).getBody();
        var inicio = cuerpo.indexOf("id=\"portada-preguntas\"");
        return cuerpo.substring(inicio, cuerpo.indexOf("</nav>", inicio));
    }
}
