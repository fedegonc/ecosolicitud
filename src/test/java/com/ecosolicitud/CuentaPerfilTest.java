package com.ecosolicitud;

import java.net.CookieManager;
import java.net.http.HttpClient;
import java.util.regex.Pattern;

import com.ecosolicitud.cuenta.interno.UsuariosRepository;
import com.ecosolicitud.shared.Rutas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CuentaPerfilTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UsuariosRepository usuarios;

    @Test
    void elPerfilExigeSesionYRedirigeAEntrar() {
        // sin sesión el perfil redirige a Ingresar; nunca muestra datos
        var r = rest.getForEntity(Rutas.CUENTA_PERFIL, String.class);
        assertThat(r.getBody()).contains("Ingresar").doesNotContain("Mi perfil");
    }

    @Test
    void elVecinoVeSusDatosYLosEditaSinTocarLaClave() {
        var sesion = new Sesion();
        sesion.post(Rutas.CUENTA_REGISTRAR, b -> {
            b.add("nombre", "PerfilAna");
            b.add("pais", "UY");
            b.add("telefono", "099 300 400");
            b.add("clave", "reciclo2026");
            b.add("condiciones", "on");
        });
        var antes = usuarios.findByNombre("perfilana").orElseThrow();

        var pagina = sesion.get(Rutas.CUENTA_PERFIL);
        assertThat(pagina.getBody()).contains("Mi perfil", "perfilana", "+598 99 300 400");

        var guardado = sesion.post(Rutas.CUENTA_PERFIL, b -> {
            b.add("nombre", "PerfilNueva");
            b.add("pais", "UY");
            b.add("telefono", "099 777 222");
            b.add("clave", "");
        });
        assertThat(guardado.getStatusCode()).isEqualTo(HttpStatus.OK);

        var despues = usuarios.findByNombre("perfilnueva").orElseThrow();
        assertThat(despues.getTelefono()).isEqualTo("59899777222");
        assertThat(despues.getClaveHash()).isEqualTo(antes.getClaveHash());

        // la redirección ya trae la confirmación y el formulario con lo nuevo
        assertThat(guardado.getBody())
                .contains("Los datos de tu cuenta quedaron guardados.",
                        "perfilnueva", "+598 99 777 222");
    }

    @Test
    void unaClaveCortaNoCambiaNada() {
        var sesion = new Sesion();
        sesion.post(Rutas.CUENTA_REGISTRAR, b -> {
            b.add("nombre", "PerfilCorta");
            b.add("pais", "UY");
            b.add("telefono", "099 500 600");
            b.add("clave", "reciclo2026");
            b.add("condiciones", "on");
        });
        var antes = usuarios.findByNombre("perfilcorta").orElseThrow();

        var r = sesion.post(Rutas.CUENTA_PERFIL, b -> {
            b.add("nombre", "PerfilCorta");
            b.add("pais", "UY");
            b.add("telefono", "099 500 600");
            b.add("clave", "123");
        });
        assertThat(r.getBody()).contains("La contraseña tiene que tener al menos 5 caracteres");

        var despues = usuarios.findByNombre("perfilcorta").orElseThrow();
        assertThat(despues.getClaveHash()).isEqualTo(antes.getClaveHash());
        assertThat(despues.getTelefono()).isEqualTo(antes.getTelefono());
    }

    // misma sesión con cookies que el registro: el POST de perfil exige el
    // CSRF de la sesión y la redirección posterior conserva la autenticación
    private class Sesion {

        private String csrf;
        private final RestTemplate propio;

        Sesion() {
            propio = new RestTemplate(new JdkClientHttpRequestFactory(
                    HttpClient.newBuilder().cookieHandler(new CookieManager())
                            .followRedirects(HttpClient.Redirect.ALWAYS).build()));
            refrescar(Rutas.CUENTA_REGISTRAR);
        }

        void refrescar(String pagina) {
            var r = propio.exchange(rest.getRootUri() + pagina, HttpMethod.GET,
                    HttpEntity.EMPTY, String.class);
            var m = Pattern.compile("name=\"_csrf\"\\s+(?:value|content)=\"([^\"]+)\"")
                    .matcher(r.getBody());
            assertThat(m.find()).isTrue();
            csrf = m.group(1);
        }

        org.springframework.http.ResponseEntity<String> get(String ruta) {
            return propio.exchange(rest.getRootUri() + ruta, HttpMethod.GET,
                    HttpEntity.EMPTY, String.class);
        }

        org.springframework.http.ResponseEntity<String> post(String ruta,
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