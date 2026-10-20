package com.ecosolicitud;

import java.util.List;
import java.util.regex.Pattern;

import com.ecosolicitud.demo.DemoService;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.solicitud.ListadoService;
import com.ecosolicitud.solicitud.SolicitudInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CopiaDatosTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private DemoService demo;

    @Autowired
    private OrganizacionService organizaciones;

    @Autowired
    private ListadoService listados;

    @BeforeEach
    void setUp() {
        demo.reiniciar();
    }

    @Test
    @DisplayName("La copia trae en .txt solo las solicitudes de la organización de la sesión")
    void copiaDeLaOrganizacionActiva() {
        var todas = organizaciones.todas();
        var propia = todas.get(0).id();
        var ajena = todas.get(1).id();
        assertThat(listados.todasDe(propia)).isNotEmpty();
        assertThat(listados.todasDe(ajena)).isNotEmpty();

        var sesion = new Sesion();
        sesion.post(Rutas.DEMO_ROL, "rol", "ORGANIZACION");
        sesion.post(Rutas.DEMO_ORGANIZACION, "id", propia);
        var r = sesion.get(Rutas.ORG_COPIA);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_PLAIN)).isTrue();
        assertThat(r.getHeaders().getContentDisposition().getFilename())
                .matches("ecosolicitud-\\d{4}-\\d{2}-\\d{2}\\.txt");
        var filas = r.getBody().lines().skip(1).map(f -> f.split("\t")[0]).toList();
        assertThat(filas).containsExactlyInAnyOrderElementsOf(ids(listados.todasDe(propia)))
                .doesNotContainAnyElementsOf(ids(listados.todasDe(ajena)));
    }

    @Test
    @DisplayName("Un ciudadano no descarga: la ruta /org lo devuelve a su inicio")
    void ciudadanoNoDescarga() {
        var r = new Sesion().get(Rutas.ORG_COPIA);
        assertThat(r.getHeaders().getContentDisposition().getFilename()).isNull();
        assertThat(r.getBody()).doesNotContain("id\tcreada");
    }

    private static List<String> ids(List<SolicitudInfo> solicitudes) {
        return solicitudes.stream().map(s -> String.valueOf(s.id())).toList();
    }

    // una sesión del navegador: cookie y token CSRF tomados de una página
    private final class Sesion {

        private final String cookie;
        private final String csrf;

        Sesion() {
            var pagina = rest.getForEntity(Rutas.MIS_SOLICITUDES, String.class);
            var m = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"").matcher(pagina.getBody());
            assertThat(m.find()).isTrue();
            csrf = m.group(1);
            cookie = pagina.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
        }

        void post(String ruta, String campo, String valor) {
            var body = new LinkedMultiValueMap<String, String>();
            body.add(campo, valor);
            body.add("_csrf", csrf);
            var headers = cabeceras();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            // TestRestTemplate sigue el redirect del post: alcanza con que no falle
            assertThat(rest.postForEntity(ruta, new HttpEntity<>(body, headers), String.class)
                    .getStatusCode().isError()).isFalse();
        }

        ResponseEntity<String> get(String ruta) {
            return rest.exchange(ruta, HttpMethod.GET, new HttpEntity<>(cabeceras()), String.class);
        }

        private HttpHeaders cabeceras() {
            var headers = new HttpHeaders();
            headers.add(HttpHeaders.COOKIE, cookie);
            return headers;
        }
    }
}
