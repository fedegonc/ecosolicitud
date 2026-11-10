package com.ecosolicitud;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"prod", "test"})
class RecursosProdTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void noHayHojasPropiasParalelas() throws IOException {
        var hojas = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:static/css/*.css");
        assertThat(Arrays.stream(hojas).map(Resource::getFilename).distinct())
                .containsExactly("app.css");
    }

    @Test
    void unaSolaHojaPropiaYCatalogoFueraDeProduccion() {
        var pagina = rest.getForObject("/", String.class);
        assertThat(Pattern.compile("<link[^>]+rel=\"stylesheet\"[^>]*>").matcher(pagina)
                .results().count()).isEqualTo(1);
        assertThat(pagina).doesNotContain("/css/design", "/css/demo", "/css/catalogo");
        assertThat(rest.getForEntity("/dev/design-system", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

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

    // un prefetch al hover guarda la lista ANTES del POST de crear/cancelar/
    // aceptar; si el redirect cayera en esa copia, la página llegaría vieja
    // (deliveryType=navigational-prefetch) y la recarga recién la actualizaría
    @Test
    void lasBandejasQueUnPostRedirigeNoSePrecargan() {
        var pagina = rest.getForObject("/", String.class);
        var reglas = Pattern.compile("speculationrules[\\s\\S]*?</script>").matcher(pagina);
        assertThat(reglas.find()).as("bloque speculationrules en el layout").isTrue();
        assertThat(reglas.group())
                .contains("\"href_matches\": \"/mis-solicitudes*\"")
                .contains("\"href_matches\": \"/org/solicitudes*\"");
    }
}
