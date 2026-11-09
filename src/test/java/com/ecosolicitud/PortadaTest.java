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
            "id=\"ds-consejos\""};

    @Autowired
    private TestRestTemplate rest;

    @Test
    void portadaRespondeConLasSecciones() {
        var r = rest.getForEntity("/?ciudad=livramento", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains(SECCIONES);
    }

    @Test
    void visitanteNoRecibeUnHistorialNiUnaInvitacionRedundante() {
        var es = rest.getForObject("/?lang=es", String.class);
        assertThat(es).contains("Todo para reciclar bien en")
                .doesNotContain("¿Querés solicitar una recolección?")
                .doesNotContain("Todavía no hiciste ninguna solicitud")
                .doesNotContain("¿Primera vez?")
                .doesNotContain("Ruta de navegación");
        var pt = rest.getForObject("/?lang=pt", String.class);
        assertThat(pt).contains("Tudo para reciclar bem em")
                .doesNotContain("Quer solicitar uma coleta?")
                .doesNotContain("Você ainda não fez nenhuma solicitação")
                .doesNotContain("Primeira vez?")
                .doesNotContain("Trilha de navegação");
    }

    @Test
    void appCssEsLaUnicaHojaPropiaEnLasPaginasPublicas() {
        for (String ruta : new String[]{Rutas.RAIZ, Rutas.ACOPIOS, Rutas.GUIA}) {
            var cuerpo = rest.getForEntity(ruta, String.class).getBody();
            assertThat(cuerpo).containsOnlyOnce("/css/app.css")
                    .doesNotContain("/css/design.css", "/css/demo.css", "/css/catalogo.css");
        }
    }
}
