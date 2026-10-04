package com.ecosolicitud;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"prod", "test"})
class RecursosProdTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void recursosVersionadosSeSirvenConCacheDeProduccion() {
        var pagina = rest.getForEntity("/", String.class);
        assertThat(pagina.getStatusCode()).isEqualTo(HttpStatus.OK);
        for (String recurso : new String[]{"/css/app.css", "/js/htmx.min.js", "/img/logo.svg"}) {
            int extension = recurso.lastIndexOf('.');
            var patron = Pattern.compile(Pattern.quote(recurso.substring(0, extension))
                    + "-[a-f0-9]{32}" + Pattern.quote(recurso.substring(extension)));
            var encontrado = patron.matcher(pagina.getBody());
            assertThat(encontrado.find()).as("URL versionada de %s", recurso).isTrue();
            var versionado = rest.getForEntity(encontrado.group(), byte[].class);
            var original = rest.getForEntity(recurso, byte[].class);
            assertThat(versionado.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(original.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(versionado.getBody()).isEqualTo(original.getBody());
            assertThat(versionado.getHeaders().getCacheControl()).contains("max-age=604800");
        }
    }
}
