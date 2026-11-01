package com.ecosolicitud;

import java.util.regex.Pattern;

import com.ecosolicitud.cuenta.interno.UsuariosRepository;
import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.solicitud.Filtro;
import com.ecosolicitud.solicitud.ListadoService;

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

    @Autowired
    private ListadoService listados;

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

    @Test
    void conErroresMuestraTodosJuntosYConservaLoEscrito() {
        var sesion = new Sesion();
        var r = sesion.post(Rutas.CUENTA_REGISTRAR, b -> {
            b.add("nombre", "María José");
            b.add("pais", "BR");
            b.add("telefono", "123");
            b.add("clave", "1234");
        });
        assertThat(r.getBody()).contains(
                "Usá solo letras sin tilde",
                "Ingresá un celular de Uruguay o Brasil",
                "La contraseña tiene que tener al menos 5 caracteres",
                "Tenés que aceptar las condiciones de uso");
        // lo escrito vuelve (menos la contraseña) y el país queda elegido
        assertThat(r.getBody()).contains("value=\"María José\"", "value=\"123\"")
                .containsPattern("value=\"BR\"\\s+selected")
                .doesNotContain("value=\"1234\"");
    }

    @Test
    void lasPaginasPrivadasLlevanAIngresarEnVezDeUn403() {
        var cliente = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        var directo = new RestTemplate(new JdkClientHttpRequestFactory(cliente));
        directo.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse res) {
                return false;
            }
        });
        for (var ruta : new String[]{Rutas.NUEVA, Rutas.MIS_SOLICITUDES, Rutas.AVISOS,
                Rutas.OPINION}) {
            var r = directo.getForEntity(rest.getRootUri() + ruta, String.class);
            assertThat(r.getStatusCode()).as(ruta).isEqualTo(HttpStatus.FOUND);
            assertThat(r.getHeaders().getLocation()).as(ruta).hasPath(Rutas.CUENTA_ENTRAR);
        }
        // lo del centro sigue cerrado sin pasar por el login de vecinos
        var org = directo.getForEntity(rest.getRootUri() + Rutas.ORG_SOLICITUDES, String.class);
        assertThat(org.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void desdeUnEnlaceAnuevaVuelveANuevaDespuesDeRegistrarse() {
        // QR → /nueva sin sesión → Ingresar → Crear cuenta → de vuelta en /nueva
        var sesion = new Sesion();
        sesion.refrescar(Rutas.NUEVA + "?ciudad=RIVERA");
        var r = sesion.post(Rutas.CUENTA_REGISTRAR, b -> {
            b.add("nombre", "vecina.qr");
            b.add("pais", "UY");
            b.add("telefono", "099 444 555");
            b.add("clave", "reciclo2026");
            b.add("condiciones", "on");
        });
        assertThat(r.getBody()).contains("name=\"ciudad\"")
                .containsPattern("value=\"RIVERA\"\\s+selected");
    }

    @Test
    void elMismoEnvioDosVecesNoDuplicaLaSolicitud() {
        var sesion = new Sesion();
        sesion.post(Rutas.CUENTA_REGISTRAR, b -> {
            b.add("nombre", "vecino.reintento");
            b.add("pais", "UY");
            b.add("telefono", "099 666 777");
            b.add("clave", "reciclo2026");
            b.add("condiciones", "on");
        });
        var id = usuarios.findByNombre("vecino.reintento").orElseThrow().getId();
        sesion.refrescar(Rutas.NUEVA);
        java.util.function.Function<String, String> enviar = direccion -> sesion.post(Rutas.NUEVA, b -> {
            b.add("ciudad", "RIVERA");
            b.add("organizacionId", "frontera-limpia");
            b.add("materiales", "PLASTICO");
            b.add("nombre", "Vecino Reintento");
            b.add("contacto", "099666777");
            b.add("direccion", direccion);
        }).getBody();
        var primera = enviar.apply("Uruguay 200");
        var segunda = enviar.apply("  uruguay 200 ");
        var numero = Pattern.compile("Solicitud #(\\d+) enviada");
        var m1 = numero.matcher(primera);
        var m2 = numero.matcher(segunda);
        assertThat(m1.find()).isTrue();
        assertThat(m2.find()).isTrue();
        // el reintento confirma la misma solicitud, sin crear otra
        assertThat(m2.group(1)).isEqualTo(m1.group(1));
        assertThat(listados.misSolicitudes(id, Filtro.TODAS, 1)).hasSize(1);
        // otra dirección es otra solicitud
        enviar.apply("Sarandí 300");
        assertThat(listados.misSolicitudes(id, Filtro.TODAS, 1)).hasSize(2);
    }

    @Test
    void conElFormularioVencidoVuelveANuevaConLosDatos() {
        var sesion = new Sesion();
        sesion.post(Rutas.CUENTA_REGISTRAR, b -> {
            b.add("nombre", "vecino.vencido");
            b.add("pais", "UY");
            b.add("telefono", "099 888 111");
            b.add("clave", "reciclo2026");
            b.add("condiciones", "on");
        });
        sesion.refrescar(Rutas.NUEVA);
        // token viejo (sesión vencida o servidor reiniciado): va primero y gana
        var r = sesion.post(Rutas.NUEVA, b -> {
            b.add("_csrf", "token-de-otra-sesion");
            b.add("ciudad", "RIVERA");
            b.add("organizacionId", "frontera-limpia");
            b.add("materiales", "PLASTICO");
            b.add("direccion", "Calle Vencida 2");
        });
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("estuvo abierta mucho tiempo",
                "value=\"Calle Vencida 2\"").doesNotContain("jsessionid");
        var id = usuarios.findByNombre("vecino.vencido").orElseThrow().getId();
        assertThat(listados.misSolicitudes(id, Filtro.TODAS, 1)).isEmpty();
    }

    @Test
    void elMismoRegistroRepetidoEntraEnVezDeDarError() {
        java.util.function.BiFunction<String, String, String> registrar = (tel, clave) ->
                new Sesion().post(Rutas.CUENTA_REGISTRAR, b -> {
                    b.add("nombre", "Vecino.Repite");
                    b.add("pais", "UY");
                    b.add("telefono", tel);
                    b.add("clave", clave);
                    b.add("condiciones", "on");
                }).getBody();
        assertThat(registrar.apply("099 321 321", "reciclo2026")).contains("Cuenta creada");
        // la respuesta se perdió y vuelve a enviar lo mismo: entra
        assertThat(registrar.apply("099321321", "reciclo2026"))
                .contains("Cuenta creada", "Mis solicitudes").doesNotContain("en uso");
        // otra clave u otro celular: no es la misma persona, sigue el error
        assertThat(registrar.apply("099 321 321", "otra-clave")).contains("en uso");
        assertThat(registrar.apply("099 999 321", "reciclo2026")).contains("en uso");
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
