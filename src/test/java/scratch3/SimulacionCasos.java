package scratch3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.ecosolicitud.EcoSolicitudApplication;
import com.ecosolicitud.cuenta.interno.UsuariosRepository;
import com.ecosolicitud.solicitud.Estado;
import com.ecosolicitud.solicitud.SolicitudService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.context.WebServerApplicationContext;

// Simulación de casos límite contra la app real (perfil test, H2 en memoria).
// Se corre con scratch-3/ejecutar.sh — nada toca la base persistente.
public final class SimulacionCasos {

    private static WebServerApplicationContext app;
    private static String base;
    private static int ok;
    private static int mal;

    public static void main(String[] args) throws Exception {
        // fase A: límite bajo a propósito → el 429 se modela explícito
        app = arrancar("--ecosolicitud.cuenta.intentos=3");
        base = "http://localhost:" + app.getWebServer().getPort();
        System.out.println("Simulación contra " + base + " (límite 3)");
        correr("tras 3 intentos de login → 429", SimulacionCasos::rateLimit);
        SpringApplication.exit(app);

        app = arrancar("--ecosolicitud.cuenta.intentos=1000");
        base = "http://localhost:" + app.getWebServer().getPort();
        System.out.println("Simulación contra " + base);

        correr("login no distingue mayúsculas", SimulacionCasos::loginMayusculas);
        correr("registro duplicado concurrente → una sola cuenta",
                SimulacionCasos::registroConcurrente);
        correr("nombre con espacios o símbolos → rechazado",
                SimulacionCasos::nombreSimbolos);
        correr("clave en los bordes (5 y 72 bytes)", SimulacionCasos::claveBorde);
        correr("token viejo tras logout → 403", SimulacionCasos::tokenTrasLogout);
        correr("cancelar solicitud ajena → 403", SimulacionCasos::cancelarAjena);
        correr("aceptar concurrente → solo una gana",
                SimulacionCasos::aceptarConcurrente);
        correr("misma cuenta en dos sesiones → ambas entran",
                SimulacionCasos::dosSesiones);

        var ex = new Experimentos(base, app);
        correr("matriz de accesos rol×ruta", () -> ex.matrizAccesos());
        correr("fuzz cartesiano de registro", () -> ex.fuzzRegistro());
        correr("FSM de sesión", () -> ex.fsmSesion());
        correr("sonda de sesiones en heap", () -> ex.memoriaSesiones());
        correr("XFF: limiter ve IP del proxy (bug)", () -> ex.proxyXff());
        correr("race aceptar×cancelar → un solo ganador", () -> ex.raceCancelarVsAceptar());
        correr("doble submit no duplica", () -> ex.dobleSubmit());
        correr("transiciones ilegales sin 500", () -> ex.transicionesIlegales());
        correr("XSS en nombre queda escapado", () -> ex.xssEscapado());
        correr("paginación en bordes sin 500", () -> ex.paginacionBordes());
        correr("aislamiento entre 3 cuentas", () -> ex.aislamientoMasivo());
        correr("seminario: 50 usuarios concurrentes", () -> ex.seminario());

        System.out.printf("%n%d escenarios bien, %d con desvíos%n", ok, mal);
        SpringApplication.exit(app);
        System.exit(mal == 0 ? 0 : 1);
    }

    private static WebServerApplicationContext arrancar(String extra) {
        return (WebServerApplicationContext) SpringApplication.run(
                EcoSolicitudApplication.class,
                "--spring.profiles.active=test", "--server.port=0", extra);
    }

    private static void rateLimit() throws Exception {
        var s = new SesionHttp(base);
        for (int i = 0; i < 3; i++) {
            s.refrescarCsrf("/cuenta/entrar");
            s.post("/cuenta/entrar",
                    Map.of("username", "nadie", "password", "mal"));
        }
        s.refrescarCsrf("/cuenta/entrar");
        var r = s.post("/cuenta/entrar",
                Map.of("username", "nadie", "password", "mal"));
        if (r.statusCode() != 429) {
            throw new IllegalStateException("esperaba 429, llegó " + r.statusCode());
        }
    }

    private static void correr(String nombre, Ejecutable e) {
        try {
            e.run();
            ok++;
            System.out.println("  ✓ " + nombre);
        } catch (Throwable t) {
            mal++;
            System.out.println("  ✗ " + nombre + " → " + t);
        }
    }

    private interface Ejecutable {
        void run() throws Exception;
    }

    private static SesionHttp login(String usuario, String clave) throws Exception {
        var s = new SesionHttp(base);
        s.refrescarCsrf("/cuenta/entrar");
        var r = s.post("/cuenta/entrar",
                Map.of("username", usuario, "password", clave));
        if (!r.body().contains("Mis solicitudes") && !r.body().contains("Solicitudes recibidas")) {
            throw new IllegalStateException("login de " + usuario + " no llegó a su inicio");
        }
        return s;
    }

    private static String registrar(SesionHttp s, String nombre,
            String telefono, String clave) throws Exception {
        s.refrescarCsrf("/cuenta/registrar");
        return s.post("/cuenta/registrar", Map.of("nombre", nombre, "pais", "UY",
                "telefono", telefono, "clave", clave, "condiciones", "on")).body();
    }

    private static void loginMayusculas() throws Exception {
        registrar(new SesionHttp(base), "casoLimaX", "099 400 001", "limonada");
        login("CASOLIMAX", "limonada");
    }

    private static void registroConcurrente() throws Exception {
        var pool = Executors.newFixedThreadPool(6);
        var captados = new ArrayList<String>();
        for (int i = 0; i < 6; i++) {
            pool.submit(() -> captados.add(registrar(new SesionHttp(base),
                    "lucha6", "099 600 600", "lucha6ok")));
        }
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        var creadas = app.getBean(UsuariosRepository.class)
                .findByNombre("lucha6").isPresent();
        if (!creadas) {
            throw new IllegalStateException("ninguna de las 6 llegó a crear");
        }
    }

    private static void nombreSimbolos() throws Exception {
        var s = new SesionHttp(base);
        for (String feo : List.of("ana maria", "ana*!@#", "mal@@@@@@")) {
            if (!registrar(s, feo, "099 500 500", "clave55").contains("nombre")) {
                throw new IllegalStateException("aceptó nombre inválido: " + feo);
            }
        }
    }

    private static void claveBorde() throws Exception {
        var s = new SesionHttp(base);
        if (registrar(s, "bordeCorto", "099 700 701", "1234").contains("Cuenta creada")) {
            throw new IllegalStateException("aceptó clave de 4");
        }
        if (registrar(s, "bordeLargo", "099 700 702", "a".repeat(73))
                .contains("Cuenta creada")) {
            throw new IllegalStateException("aceptó clave de 73 bytes");
        }
        if (!registrar(s, "bordeExacto", "099 700 703", "a".repeat(72))
                .contains("Cuenta creada")) {
            throw new IllegalStateException("rechazó clave válida de 72 bytes");
        }
    }

    private static void tokenTrasLogout() throws Exception {
        registrar(new SesionHttp(base), "fueraToken", "099 800 800", "fuera88");
        var s = login("fueraToken", "fuera88");
        s.post("/cuenta/salir", Map.of());
        var r = s.post("/nueva", Map.of("ciudad", "RIVERA", "materiales", "PLASTICO",
                "organizacionId", "frontera-limpia", "nombre", "x", "contacto", "099",
                "direccion", "x 1"));
        if (r.statusCode() != 403) {
            throw new IllegalStateException("token viejo aceptado: " + r.statusCode());
        }
    }

    private static void cancelarAjena() throws Exception {
        var propia = registrar(new SesionHttp(base), "duenia9", "099 900 901", "duenia9");
        if (!propia.contains("Cuenta creada")) {
            throw new IllegalStateException("registro de dueña falló: " + propia.length());
        }
        var s = login("duenia9", "duenia9");
        s.refrescarCsrf("/nueva");
        s.post("/nueva", Map.of("ciudad", "RIVERA", "materiales", "PLASTICO",
                "organizacionId", "frontera-limpia", "nombre", "D", "contacto", "099",
                "direccion", "x 1"));
        var lista = app.getBean(SolicitudService.class).todas();
        var ultima = lista.get(lista.size() - 1);
        registrar(new SesionHttp(base), "intrusa9", "099 900 902", "intrusa9");
        var mal = login("intrusa9", "intrusa9");
        mal.refrescarCsrf("/nueva");
        var r = mal.post("/mis-solicitudes/" + ultima.id() + "/cancelar",
                Map.of("version", "0"));
        if (r.statusCode() != 403) {
            throw new IllegalStateException("canceló lo ajeno: " + r.statusCode());
        }
    }

    private static void aceptarConcurrente() throws Exception {
        var id = app.getBean(SolicitudService.class).todas().stream()
                .filter(s -> s.estado() == Estado.PENDIENTE
                        && s.organizacionNombre().contains("Paso del Enano"))
                .findFirst().orElseThrow().id();
        var org = login("renacer-norte", "centro-demo");
        org.refrescarCsrf("/org/solicitudes");
        var pool = Executors.newFixedThreadPool(2);
        var r1 = pool.submit(() -> org.post("/org/solicitudes/" + id + "/aceptar",
                Map.of("version", "0")));
        var r2 = pool.submit(() -> org.post("/org/solicitudes/" + id + "/aceptar",
                Map.of("version", "0")));
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        var fin = app.getBean(SolicitudService.class).todas().stream()
                .filter(s -> s.id() == id).findFirst().orElseThrow();
        if (fin.estado() != Estado.EN_CURSO || r1.get().statusCode() == 500
                || r2.get().statusCode() == 500) {
            throw new IllegalStateException("estado final " + fin.estado()
                    + " / " + r1.get().statusCode() + "," + r2.get().statusCode());
        }
    }

    private static void dosSesiones() throws Exception {
        registrar(new SesionHttp(base), "gemela77", "099 770 770", "gemela7");
        login("gemela77", "gemela7");
        login("gemela77", "gemela7");
    }
}
