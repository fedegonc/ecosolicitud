package com.ecosolicitud;

import java.util.regex.Pattern;

import com.ecosolicitud.cuenta.Cuentas;
import com.ecosolicitud.cuenta.Pais;
import com.ecosolicitud.solicitud.ListadoService;
import com.ecosolicitud.solicitud.Filtro;
import com.ecosolicitud.shared.Rutas;

import java.net.CookieManager;
import java.net.http.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LoginTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private Cuentas cuentas;

    @Autowired
    private ListadoService listados;

    private String usuarioId;

    @BeforeEach
    void cuenta() {
        if (listados.misSolicitudes("test-ciudadana", Filtro.TODAS, 1).isEmpty()
                && cuentas.buscar("099 777 000", Pais.UY).isEmpty()) {
            usuarioId = cuentas.registrarCiudadano("ana-prueba", "099 777 000",
                    Pais.UY, "reciclo2026").id();
        } else {
            usuarioId = cuentas.buscar("099 777 000", Pais.UY).orElseThrow().id();
        }
    }

    @Test
    void loginUsaLaIdentidadRealYLogoutVuelveAlDemo() {
        var sesion = new Sesion("/cuenta/entrar");

        var login = sesion.post("/cuenta/entrar", b -> {
            b.add("username", "Ana-Prueba");
            b.add("password", "reciclo2026");
        });
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).contains("Mis solicitudes");
        sesion.refrescar(Rutas.NUEVA);

        sesion.post(Rutas.NUEVA, b -> {
            b.add("ciudad", "RIVERA");
            b.add("organizacionId", "frontera-limpia");
            b.add("materiales", "PLASTICO");
            b.add("nombre", "ana-prueba");
            b.add("contacto", "099777000");
            b.add("direccion", "Uruguay 100");
        });
        assertThat(listados.misSolicitudes(usuarioId, Filtro.TODAS, 1))
                .hasSize(1).first().extracting(s -> s.nombreCiudadano())
                .isEqualTo("ana-prueba");

        sesion.post("/cuenta/salir", b -> {
        });
        // sin sesión ya no puede crear: el anónimo no hereda el ciudadano demo
        sesion.refrescar(Rutas.RAIZ);
        assertThatThrownBy(() -> sesion.post(Rutas.NUEVA, b -> {
        })).isInstanceOf(HttpClientErrorException.Forbidden.class);
        assertThat(listados.misSolicitudes(usuarioId, Filtro.TODAS, 1)).hasSize(1);
    }

    @Test
    void claveIncorrectaNoAutentica() {
        var sesion = new Sesion("/cuenta/entrar");
        sesion.refrescar(Rutas.RAIZ);
        int antes = listados.misSolicitudes(usuarioId, Filtro.TODAS, 1).getContent().size();
        var login = sesion.post("/cuenta/entrar", b -> {
            b.add("username", "ana-prueba");
            b.add("password", "incorrecta");
        });
        assertThat(login.getBody()).contains("incorrectos");
        // sin sesión, la creación queda bloqueada (el leak del ciudadano demo)
        assertThatThrownBy(() -> sesion.post(Rutas.NUEVA, b -> {
        })).isInstanceOf(HttpClientErrorException.Forbidden.class);
        assertThat(listados.misSolicitudes(usuarioId, Filtro.TODAS, 1)
                .getContent()).hasSize(antes);
    }

    private class Sesion {
        private String csrf;
        private final RestTemplate propio;

        Sesion(String pagina) {
            propio = new RestTemplate(new JdkClientHttpRequestFactory(
                    HttpClient.newBuilder().cookieHandler(new CookieManager())
                            .followRedirects(HttpClient.Redirect.ALWAYS).build()));
            refrescar(pagina);
        }

        void refrescar(String pagina) {
            var r = propio.exchange(rest.getRootUri() + pagina, HttpMethod.GET,
                    HttpEntity.EMPTY, String.class);
            var m = Pattern.compile("name=\"_csrf\"\\s+(?:value|content)=\"([^\"]+)\"")
                    .matcher(r.getBody());
            assertThat(m.find()).isTrue();
            csrf = m.group(1);
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
