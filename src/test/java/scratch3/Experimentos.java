package scratch3;

import java.util.List;
import java.util.Map;

import org.springframework.boot.web.context.WebServerApplicationContext;

// Matriz de accesos + fuzz + FSM + sondas: cobertura por construcción.
final class Experimentos {

    interface Ejecutable {
        void run() throws Exception;
    }

    private final String base;
    private final WebServerApplicationContext app;

    Experimentos(String base, WebServerApplicationContext app) {
        this.base = base;
        this.app = app;
    }

    void correr(String nombre, Ejecutable e, int[] cont) throws Exception {
        try {
            e.run();
            cont[0]++;
            System.out.println("  ✓ " + nombre);
        } catch (Throwable t) {
            cont[1]++;
            System.out.println("  ✗ " + nombre + " → " + t);
        }
    }

    // rol × ruta × método contra tabla de política
    void matrizAccesos() throws Exception {
        var ciudadano = Sesiones.login(base, app, "matrizC", "099 111 222", "matrizC1");
        var org = Sesiones.loginOrg(base);
        var demo = Sesiones.loginDemo(base);
        var anon = new SesionHttp(base);

        record Fila(String ruta, int anon, int ciu, int org, int demo) {}
        var filas = List.of(
                new Fila("/", 200, 200, 200, 200),
                new Fila("/acopios", 200, 200, 200, 200),
                new Fila("/guia", 200, 200, 200, 200),
                new Fila("/comunidad", 200, 200, 200, 200),
                new Fila("/estadisticas", 200, 200, 200, 200),
                new Fila("/cuenta/entrar", 200, 200, 200, 200),
                new Fila("/cuenta/registrar", 200, 200, 200, 200),
                new Fila("/cuenta/condiciones", 200, 200, 200, 200),
                new Fila("/modo/dev", 200, 200, 200, 200),
                new Fila("/opinion", 403, 200, 200, 200),
                new Fila("/nueva", 403, 200, 200, 200),
                new Fila("/mis-solicitudes", 403, 200, 200, 200),
                new Fila("/avisos", 403, 200, 200, 200),
                new Fila("/org/solicitudes", 403, 403, 200, 200),
                new Fila("/org/perfil", 403, 403, 200, 200));

        for (Fila f : filas) {
            esperar(anon, f.ruta(), f.anon(), "anon", f.ruta());
            esperar(ciudadano, f.ruta(), f.ciu(), "ciudadano", f.ruta());
            esperar(org, f.ruta(), f.org(), "org", f.ruta());
            esperar(demo, f.ruta(), f.demo(), "demo", f.ruta());
        }
    }

    private static void esperar(SesionHttp s, String ruta, int esperado,
            String quien, String label) throws Exception {
        var st = s.get(ruta).statusCode();
        if (st != esperado) {
            throw new IllegalStateException(quien + " " + label + " → " + st
                    + " (esperaba " + esperado + ")");
        }
    }

    // fuzz cartesiano: producto de bordes contra la spec
    void fuzzRegistro() throws Exception {
        var casos = Map.of(
                "abc9", false, "espacio nombre", false, "bien-99", true);
        var i = 0;
        for (var e : casos.entrySet()) {
            i++;
            var s = new SesionHttp(base);
            s.refrescarCsrf("/cuenta/registrar");
            var cuerpo = s.post("/cuenta/registrar", Map.of("nombre", e.getKey(),
                    "pais", "UY", "telefono", "099 555 10" + i, "clave", "clave55",
                    "condiciones", "on")).body();
            var ok = cuerpo.contains("Cuenta creada");
            if (ok != e.getValue()) {
                throw new IllegalStateException("nombre " + e.getKey() + " → "
                        + (ok ? "aceptado" : "rechazado"));
            }
        }
    }

    // FSM de sesión: transiciones válidas solamente
    void fsmSesion() throws Exception {
        var s = new SesionHttp(base);
        esperar(s, "/nueva", 403, "fsm", "nueva-antes-de-login");
        s.refrescarCsrf("/cuenta/registrar");
        s.post("/cuenta/registrar", Map.of("nombre", "fsmuser", "pais", "UY",
                "telefono", "099 222 333", "clave", "fsm555", "condiciones", "on"));
        s.refrescarCsrf("/cuenta/entrar");
        s.post("/cuenta/entrar", Map.of("username", "fsmuser", "password", "fsm555"));
        esperar(s, "/nueva", 200, "fsm", "nueva-logueado");
        s.refrescarCsrf("/nueva");
        s.post("/cuenta/salir", Map.of());
        esperar(s, "/nueva", 403, "fsm", "nueva-tras-logout");
    }

    // sonda: sesiones nuevas acumuladas en heap (cada GET crea ActorSesion)
    void memoriaSesiones() throws Exception {
        var rt = Runtime.getRuntime();
        System.gc();
        var antes = rt.totalMemory() - rt.freeMemory();
        for (int i = 0; i < 40; i++) {
            new SesionHttp(base).get("/");
        }
        System.gc();
        var delta = (rt.totalMemory() - rt.freeMemory() - antes) / 1024 / 1024;
        System.out.println("    heap +40 sesiones ≈ " + delta + " MiB");
    }

    // race: org acepta mientras ciudadano cancela → un solo ganador
    void raceCancelarVsAceptar() throws Exception {
        var c = Sesiones.login(base, app, "raceC", "099 444 555", "raceC55");
        var id = crearSolicitud(c, "raceC");
        var org = Sesiones.loginOrg(base);
        org.refrescarCsrf("/org/solicitudes");
        var resultados = new java.util.concurrent.CopyOnWriteArrayList<Integer>();
        java.util.concurrent.Callable<Integer>[] acciones = new java.util.concurrent.Callable[]{
                () -> {
                    var v = versionDe(org.get("/org/solicitudes").body(), id);
                    return org.post("/org/solicitudes/" + id + "/aceptar",
                            Map.of("version", String.valueOf(v))).statusCode();
                },
                () -> {
                    c.refrescarCsrf("/mis-solicitudes");
                    var v = versionDe(c.get("/mis-solicitudes").body(), id);
                    return c.post("/mis-solicitudes/" + id + "/cancelar",
                            Map.of("version", String.valueOf(v))).statusCode();
                }};
        var hilos = new java.util.ArrayList<Thread>();
        for (var a : acciones) {
            hilos.add(new Thread(() -> {
                try {
                    resultados.add(a.call());
                } catch (Exception e) {
                    resultados.add(-1);
                }
            }));
        }
        hilos.forEach(Thread::start);
        for (var h : hilos) h.join();
        var detalle = org.get("/org/solicitudes/" + id).body();
        var fin2 = java.util.regex.Pattern.compile(
                "(?i)(aceptada|cancelada|pendiente|rechazada)").matcher(detalle);
        var estado = fin2.find() ? fin2.group(1).toUpperCase() : "?";
        if (!"ACEPTADA".equals(estado) && !"CANCELADA".equals(estado)
                && !"RECHAZADA".equals(estado)) {
            throw new IllegalStateException("estado final inválido: " + estado
                    + " (resultados " + resultados + ")");
        }
        System.out.println("    race aceptar×cancelar → estado final " + estado);
    }

    private static int versionDe(String html, int id) {
        var m = java.util.regex.Pattern.compile(
                "/" + id + "/[a-z]+[\\s\\S]{0,400}?name=\"version\"[^>]*value=\"(\\d+)\"")
                .matcher(html);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private int crearSolicitud(SesionHttp c, String nombre) throws Exception {
        c.refrescarCsrf("/nueva");
        var r = c.post("/nueva", Map.of("ciudad", "RIVERA",
                "organizacionId", "frontera-limpia", "materiales", "PLASTICO",
                "nombre", nombre, "contacto", "099000001",
                "direccion", "Calle 1"));
        var m = java.util.regex.Pattern.compile("/mis-solicitudes/(\\d+)")
                .matcher(r.headers().firstValue("location").orElse(""));
        if (m.find()) return Integer.parseInt(m.group(1));
        var m2 = java.util.regex.Pattern.compile("mis-solicitudes/(\\d+)")
                .matcher(r.body());
        if (m2.find()) return Integer.parseInt(m2.group(1));
        throw new IllegalStateException("sin id de solicitud: " + r.statusCode());
    }

    // doble submit: un doble-clic no debería crear dos solicitudes
    void dobleSubmit() throws Exception {
        var c = Sesiones.login(base, app, "dobleC", "099 666 777", "dobleC55");
        c.refrescarCsrf("/nueva");
        var body = Map.of("ciudad", "RIVERA", "organizacionId",
                "frontera-limpia", "materiales", "PLASTICO", "nombre", "dobleC",
                "contacto", "099111222", "direccion", "Calle 9");
        c.post("/nueva", body);
        c.post("/nueva", body);
        var pagina = c.get("/mis-solicitudes").body();
        var links = java.util.regex.Pattern
                .compile("/mis-solicitudes/\\d+").matcher(pagina).results().count();
        if (links > 1) {
            throw new IllegalStateException("creadas=" + links
                    + " — el doble submit no es idempotente");
        }
        if (links == 0) {
            throw new IllegalStateException("no se creó ninguna solicitud");
        }
    }

    // transiciones ilegales del estado: nunca 500
    void transicionesIlegales() throws Exception {
        var c = Sesiones.login(base, app, "transC", "099 888 999", "transC55");
        var id = crearSolicitud(c, "transC");
        var org = Sesiones.loginOrg(base);
        org.refrescarCsrf("/org/solicitudes");
        var st1 = org.post("/org/solicitudes/" + id + "/completar", Map.of())
                .statusCode();
        var st2 = org.post("/org/solicitudes/" + id + "/rechazar", Map.of())
                .statusCode();
        org.post("/org/solicitudes/" + id + "/aceptar", Map.of());
        var st3 = org.post("/org/solicitudes/" + id + "/rechazar", Map.of())
                .statusCode();
        for (var st : new int[]{st1, st2, st3}) {
            if (st == 500) throw new IllegalStateException("500 en transición");
        }
        if (st3 == 302) {
            throw new IllegalStateException("rechazar tras aceptar no falló");
        }
    }

    // XSS: datos del usuario deben salir escapados
    void xssEscapado() throws Exception {
        var c = Sesiones.login(base, app, "xss<script>", "099 101 202", "xss5555");
        var pagina = c.get("/").body();
        if (pagina.contains("<script>alert") || pagina.contains("xss<script>")) {
            throw new IllegalStateException("nombre renderizado sin escapar");
        }
    }

    // bordes de paginación: nunca 500
    void paginacionBordes() throws Exception {
        var c = Sesiones.login(base, app, "pagiC", "099 303 404", "pagiC55");
        for (var p : new String[]{"-1", "0", "abc", "9999", "1.5"}) {
            var st = c.get("/mis-solicitudes?page=" + p).statusCode();
            if (st == 500) throw new IllegalStateException("page=" + p + " → 500");
        }
    }

    // aislamiento a escala: cada cuenta solo ve lo suyo
    void aislamientoMasivo() throws Exception {
        for (var u : new String[]{"iso1", "iso2", "iso3"}) {
            var c = Sesiones.login(base, app, u, "099 505 " + u.charAt(3) + "06", u + "55");
            crearSolicitud(c, u);
        }
        for (var u : new String[]{"iso1", "iso2", "iso3"}) {
            var c = Sesiones.login(base, app, u, "099 505 " + u.charAt(3) + "06", u + "55");
            var pagina = c.get("/mis-solicitudes").body();
            for (var otro : new String[]{"iso1", "iso2", "iso3"}) {
                if (!otro.equals(u) && pagina.contains(otro)) {
                    throw new IllegalStateException(u + " ve datos de " + otro);
                }
            }
        }
    }

    // Seminario: N usuarios de una sala con la misma IP hacen
    // registro → login → solicitud → listado, en paralelo.
    void seminario() throws Exception {
        var n = 50;
        var tiempos = new java.util.concurrent.ConcurrentLinkedDeque<Long>();
        var estados = new java.util.concurrent.ConcurrentLinkedDeque<String>();
        var pool = java.util.concurrent.Executors.newFixedThreadPool(16);
        var listo = new java.util.concurrent.CountDownLatch(n);
        var t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            var num = i;
            pool.submit(() -> {
                var t = System.nanoTime();
                try {
                    var usuario = "sem" + num;
                    var tel = "099 7" + String.format("%05d", num);
                    var c = Sesiones.login(base, app, usuario, tel, "sem5" + num);
                    c.refrescarCsrf("/nueva");
                    var r = c.post("/nueva", Map.of("ciudad", "RIVERA",
                            "organizacionId", "frontera-limpia",
                            "materiales", "PLASTICO", "nombre", usuario,
                            "contacto", tel, "direccion", "Aula 1"));
                    estados.add(usuario + ":" + r.statusCode());
                    c.get("/mis-solicitudes");
                } catch (Exception e) {
                    estados.add("sem" + num + ":EX:" + e.getMessage());
                } finally {
                    tiempos.add((System.nanoTime() - t) / 1_000_000);
                    listo.countDown();
                }
            });
        }
        listo.await();
        pool.shutdown();
        var total = (System.nanoTime() - t0) / 1_000_000;
        var ok = estados.stream().filter(e -> e.endsWith(":302")
                || e.endsWith(":200")).count();
        var x429 = estados.stream().filter(e -> e.endsWith(":429")).count();
        var x500 = estados.stream().filter(e -> e.contains(":5")).count();
        var errores = estados.stream().filter(e -> e.contains(":EX:")).count();
        var lat = tiempos.stream().mapToLong(Long::longValue).sorted().toArray();
        var p95 = lat[(int) (lat.length * 0.95)];
        System.out.printf("    %d usuarios: %d ok, %d 429, %d 5xx, %d errores | "
                + "total %d ms, p95 %d ms%n", n, ok, x429, x500, errores, total, p95);
        if (x500 > 0 || errores > 0) {
            throw new IllegalStateException("5xx=" + x500 + " errores=" + errores
                    + " → " + estados.stream().filter(e -> !e.endsWith(":302")
                            && !e.endsWith(":200")).limit(5).toList());
        }
    }

    // XFF: el limiter debería ver la IP real; si 429 sale igual → bug del proxy
    void proxyXff() throws Exception {
        var s = new SesionHttp(base);
        for (int i = 0; i < 4; i++) {
            s.refrescarCsrf("/cuenta/entrar");
            var r = s.post("/cuenta/entrar",
                    Map.of("username", "xff" + i, "password", "mal"));
            if (r.statusCode() == 429 && i < 3) {
                throw new IllegalStateException("429 en intento " + i
                        + " → el limiter agrupa por IP del proxy, no por cliente");
            }
        }
    }
}
