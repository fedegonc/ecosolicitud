package scratch3;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Cliente con cookies y CSRF de verdad: replica lo que hace un navegador.
final class SesionHttp {

    private final String base;
    private final HttpClient http;
    private String csrf;

    String ultimoCsrf() {
        return csrf;
    }

    SesionHttp(String base) {
        this.base = base;
        http = HttpClient.newBuilder().cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.ALWAYS).build();
    }

    String pagina(String ruta) throws IOException, InterruptedException {
        return get(ruta).body();
    }

    HttpResponse<String> get(String ruta) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(URI.create(base + ruta)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    void refrescarCsrf(String ruta) throws IOException, InterruptedException {
        var m = Pattern.compile("name=\"_csrf\"\\s+(?:value|content)=\"([^\"]+)\"")
                .matcher(pagina(ruta));
        if (!m.find()) {
            throw new IllegalStateException("sin token CSRF en " + ruta);
        }
        csrf = m.group(1);
    }

    HttpResponse<String> post(String ruta, Map<String, String> campos)
            throws IOException, InterruptedException {
        var cuerpo = campos.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        if (csrf != null) {
            cuerpo += "&_csrf=" + URLEncoder.encode(csrf, StandardCharsets.UTF_8);
        }
        var r = HttpRequest.newBuilder(URI.create(base + ruta))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build();
        return http.send(r, HttpResponse.BodyHandlers.ofString());
    }
}
