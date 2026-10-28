package com.ecosolicitud;

import java.net.CookieManager;
import java.net.http.HttpClient;
import java.util.regex.Pattern;

import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.solicitud.Estado;
import com.ecosolicitud.solicitud.SolicitudService;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CentroLoginTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private SolicitudService servicio;

    private Sesion org;

    @BeforeEach
    void login() {
        org = new Sesion();
        org.refrescar("/cuenta/entrar");
        org.post("/cuenta/entrar", b -> {
            b.add("username", "renacer-norte");
            b.add("password", "centro-demo");
        });
        org.refrescar(Rutas.NUEVA);
    }

    @Test
    void elCentroVeSuBandejaYActuaSoloSobreSusSolicitudes() {
        var pagina = org.get(Rutas.ORG_SOLICITUDES);
        assertThat(pagina.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pagina.getBody()).contains("Solicitudes recibidas");

        var propia = servicio.todas().stream()
                .filter(s -> s.estado() == Estado.PENDIENTE
                        && s.organizacionNombre().contains("Paso del Enano"))
                .findFirst().orElseThrow();
        org.refrescar(Rutas.ORG_SOLICITUDES);
        var r = org.post(Rutas.expandir(Rutas.ORG_ACEPTAR, propia.id()),
                b -> b.add("version", String.valueOf(propia.version())));
        assertThat(r.getBody()).contains("aceptada");

        var ajena = servicio.todas().stream()
                .filter(s -> !s.organizacionNombre().contains("Paso del Enano"))
                .findFirst().orElseThrow();
        var r2 = org.post(Rutas.expandir(Rutas.ORG_ACEPTAR, ajena.id()),
                b -> b.add("version", "0"));
        assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private class Sesion {
        private String csrf;
        private final RestTemplate propio;

        Sesion() {
            propio = new RestTemplate(new JdkClientHttpRequestFactory(
                    HttpClient.newBuilder().cookieHandler(new CookieManager())
                            .followRedirects(HttpClient.Redirect.ALWAYS).build()));
            propio.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
                @Override
                public boolean hasError(
                        org.springframework.http.client.ClientHttpResponse r) {
                    return false;
                }
            });
        }

        void refrescar(String pagina) {
            var m = Pattern.compile("name=\"_csrf\"\\s+(?:value|content)=\"([^\"]+)\"")
                    .matcher(get(pagina).getBody());
            assertThat(m.find()).isTrue();
            csrf = m.group(1);
        }

        ResponseEntity<String> get(String ruta) {
            return propio.exchange(rest.getRootUri() + ruta, HttpMethod.GET,
                    HttpEntity.EMPTY, String.class);
        }

        ResponseEntity<String> post(String ruta,
                java.util.function.Consumer<LinkedMultiValueMap<String, String>> fill) {
            var body = new LinkedMultiValueMap<String, String>();
            fill.accept(body);
            body.add("_csrf", csrf);
            var h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            return propio.postForEntity(rest.getRootUri() + ruta,
                    new HttpEntity<>(body, h), String.class);
        }
    }
}
