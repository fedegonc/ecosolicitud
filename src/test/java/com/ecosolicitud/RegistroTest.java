package com.ecosolicitud;

import java.util.regex.Pattern;

import com.ecosolicitud.cuenta.interno.UsuariosRepository;
import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.shared.Rutas;

import java.net.CookieManager;
import java.net.http.HttpClient;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RegistroTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UsuariosRepository usuarios;

    @Test
    void registrarCreaLaCuentaIniciaSesionYLlevaAlPanel() {
        var sesion = new Sesion();
        var r = sesion.post(Rutas.CUENTA_REGISTRAR, b -> {
            b.add("nombre", "AnaPrueba");
            b.add("pais", "UY");
            b.add("telefono", "099 123 456");
            b.add("clave", "reciclo2026");
            b.add("condiciones", "on");
        });
        // misma entrada que el login: redirect a la sección de inicio del rol,
        // el cliente sigue el 302 con la sesión rotada y ya autenticada
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("Mis solicitudes", "anaprueba",
                "Cuenta creada").doesNotContain("Crear cuenta", "Ingresar");

        var u = usuarios.findByNombre("anaprueba").orElseThrow();
        assertThat(u.getRol()).isEqualTo(Rol.CIUDADANO);
        assertThat(u.getClaveHash()).doesNotContain("reciclo2026").startsWith("$2");
    }

    @Test
    void casosLimiteDevuelvenElFormularioConError() {
        var sesion = new Sesion();
        sesion.post(Rutas.CUENTA_REGISTRAR, b -> {
            b.add("nombre", "AnaNueva");
            b.add("pais", "UY");
            b.add("telefono", "099 222 222");
            b.add("clave", "reciclo2026");
            b.add("condiciones", "on");
        });
        // nombre repetido sin importar mayúsculas
        var duplicado = sesion.post(Rutas.CUENTA_REGISTRAR, b -> {
            b.add("nombre", "ANANUEVA");
            b.add("pais", "UY");
            b.add("telefono", "099 999 999");
            b.add("clave", "reciclo2026");
            b.add("condiciones", "on");
        });
        assertThat(duplicado.getBody()).contains("en uso");
        // nombre corto
        var corto = sesion.post(Rutas.CUENTA_REGISTRAR, b -> {
            b.add("nombre", "ana");
            b.add("pais", "UY");
            b.add("telefono", "099 111 111");
            b.add("clave", "reciclo2026");
            b.add("condiciones", "on");
        });
        assertThat(corto.getBody()).contains("5 caracteres");
        // nombre con espacio
        var espacio = sesion.post(Rutas.CUENTA_REGISTRAR, b -> {
            b.add("nombre", "ana maria");
            b.add("pais", "UY");
            b.add("telefono", "099 111 111");
            b.add("clave", "reciclo2026");
            b.add("condiciones", "on");
        });
        assertThat(espacio.getBody()).contains("espacios");
        // clave corta + teléfono inválido + sin condiciones
        var invalido = sesion.post(Rutas.CUENTA_REGISTRAR, b -> {
            b.add("nombre", "valido99");
            b.add("pais", "UY");
            b.add("telefono", "juan perez");
            b.add("clave", "1234");
        });
        assertThat(invalido.getBody()).contains("celular", "5 caracteres",
                "condiciones");
        assertThat(usuarios.findByNombre("valido99")).isEmpty();
    }

    private class Sesion {
        private String csrf;
        private final RestTemplate propio;

        // con cookie handler porque el registro rota el id de sesión:
        // el siguiente request ya va con la sesión autenticada nueva
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
