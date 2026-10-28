package scratch3;

import java.util.Map;

import org.springframework.boot.web.context.WebServerApplicationContext;
import com.ecosolicitud.cuenta.interno.UsuariosRepository;

// helpers de sesión para la simulación: registro + login real por formulario
final class Sesiones {

    private Sesiones() {
    }

    static SesionHttp login(String base, WebServerApplicationContext app,
            String usuario, String telefono, String clave) throws Exception {
        if (app.getBean(UsuariosRepository.class).findByNombre(usuario).isEmpty()) {
            var r = new SesionHttp(base);
            r.refrescarCsrf("/cuenta/registrar");
            var reg = r.post("/cuenta/registrar",
                    Map.of("nombre", usuario, "pais", "UY",
                            "telefono", telefono, "clave", clave,
                            "condiciones", "on"));
            if (reg.body() == null || !reg.body().contains("Cuenta creada")) {
                throw new IllegalStateException("registro " + usuario
                        + " → " + reg.statusCode() + " "
                        + java.util.regex.Pattern.compile("<title>([^<]*)")
                                .matcher(reg.body() == null ? "" : reg.body())
                                .replaceAll("$1"));
            }
        }
        var s = new SesionHttp(base);
        s.refrescarCsrf("/cuenta/entrar");
        var r = s.post("/cuenta/entrar",
                Map.of("username", usuario, "password", clave));
        if (r.statusCode() != 200 && r.statusCode() != 302) {
            throw new IllegalStateException("login " + usuario
                    + " → " + r.statusCode());
        }
        if (r.body() != null && r.body().contains("incorrectos")) {
            throw new IllegalStateException("login " + usuario
                    + " → credenciales rechazadas (registro falló)");
        }
        return s;
    }

    static SesionHttp loginOrg(String base) throws Exception {
        var s = new SesionHttp(base);
        s.refrescarCsrf("/cuenta/entrar");
        s.post("/cuenta/entrar",
                Map.of("username", "renacer-norte", "password", "centro-demo"));
        return s;
    }

    static SesionHttp loginDemo(String base) throws Exception {
        var s = new SesionHttp(base);
        s.refrescarCsrf("/modo/dev");
        s.post("/modo/dev", Map.of("username", "admin-test",
                "password", "clave-solo-de-pruebas"));
        return s;
    }
}
