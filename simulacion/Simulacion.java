// Simulación en memoria de EcoSolicitud: la misma lógica de dominio que
// src/main/java, sin Spring, JPA ni HTTP. Cada clase lleva junto a su nombre
// el archivo real que replica. Se ejecuta con:
//
//     java simulacion/Simulacion.java
//
// Sustituciones conscientes (el contrato se conserva, la pieza externa no):
//   - BCryptPasswordEncoder -> Cifrado (SHA-256 con sal; mismo encode/matches)
//   - Base de datos JPA     -> listas/mapas en memoria con el mismo orden y @Version
//   - HttpSession           -> Sesion (autenticado + ActorSesion)
//   - Bean Validation       -> las mismas reglas chequeadas a mano, mismos mensajes
//   - Markdown/HTML         -> el cuerpo de comunidad se guarda tal cual
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class Simulacion {

    // ======================================================================
    // shared/
    // ======================================================================

    // shared/Rol.java
    enum Rol { CIUDADANO, ORGANIZACION }

    // shared/Ciudad.java
    enum Ciudad {
        RIVERA("Rivera"), LIVRAMENTO("Sant'Ana do Livramento");
        private final String nombre;
        Ciudad(String nombre) { this.nombre = nombre; }
        public String nombre() { return nombre; }
    }

    // cuenta/Pais.java
    enum Pais {
        UY("598"), BR("55");
        private final String codigo;
        Pais(String codigo) { this.codigo = codigo; }
        public String codigo() { return codigo; }
    }

    // shared/Limites.java
    static final class Limites {
        static final int NOMBRE = 80;
        static final int CONTACTO = 40;
        static final int DIRECCION = 120;
        static final int REFERENCIA = 120;
        static final int NOTA = 300;
        static final int HORARIO = 80;
        static final int TELEFONO = 30;
        static final int RESPUESTA_TEXTO = 1000;
        private Limites() { }
    }

    // shared/Fechas.java
    static final class Fechas {
        static final ZoneId ZONA = ZoneId.of("America/Montevideo");
        private static final DateTimeFormatter CORTA =
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        static String corta(Instant instante) {
            return instante == null ? "" : CORTA.format(instante.atZone(ZONA));
        }
    }

    // shared/Ubicacion.java
    record Ubicacion(double latitud, double longitud) {
        Ubicacion {
            if (!(latitud >= -90 && latitud <= 90 && longitud >= -180 && longitud <= 180)) {
                throw new IllegalArgumentException("Coordenadas fuera de rango");
            }
        }
        String enlaceMapa() {
            return String.format(Locale.ROOT,
                    "https://www.openstreetmap.org/?mlat=%.5f&mlon=%.5f#map=17/%.5f/%.5f",
                    latitud, longitud, latitud, longitud);
        }
    }

    // shared/Rutas.java (subconjunto que usa la simulación)
    static final class Rutas {
        static final String RAIZ = "/";
        static final String NUEVA = "/nueva";
        static final String NUEVA_VALIDAR = NUEVA + "/validar";
        static final String MIS_SOLICITUDES = "/mis-solicitudes";
        static final String MIS_INFORME = MIS_SOLICITUDES + "/informe";
        static final String ACOPIOS = "/acopios";
        static final String COMUNIDAD = "/comunidad";
        static final String ESTADISTICAS = "/estadisticas";
        static final String OPINION = "/opinion";
        static final String OPINION_RESPALDO = OPINION + "/respaldo";
        static final String AVISOS = "/avisos";
        static final String AVISOS_CONTADOR = AVISOS + "/contador";
        static final String ORG_SOLICITUDES = "/org/solicitudes";
        static final String ORG_ARCHIVADAS = ORG_SOLICITUDES + "/archivadas";
        static final String ORG_INFORME = ORG_SOLICITUDES + "/informe";
        static final String ORG_COPIA = ORG_SOLICITUDES + "/copia";
        static final String ORG_PERFIL = "/org/perfil";
        static final String CUENTA_REGISTRAR = "/cuenta/registrar";
        static final String CUENTA_ENTRAR = "/cuenta/entrar";
        static final String CUENTA_SALIR = "/cuenta/salir";
        private Rutas() { }
    }

    // shared/Seccion.java (lo que decide el flujo)
    static final class Seccion {
        static String inicioDe(Rol rol) {
            return rol == Rol.ORGANIZACION ? Rutas.ORG_SOLICITUDES : Rutas.MIS_SOLICITUDES;
        }
    }

    // shared/Actor.java
    record Actor(Rol rol, String ciudadanoId, String nombreCiudadano,
            String organizacionId) {
        boolean esOrganizacion() { return rol == Rol.ORGANIZACION; }
        boolean esCiudadano() { return rol == Rol.CIUDADANO; }
    }

    // shared/ActorSesion.java: los valores por defecto son los del modo demo
    // anónimo (visitante de la portada), igual que en la app real.
    static class ActorSesion {
        private Rol rol = Rol.CIUDADANO;
        private String organizacionId;
        private String ciudadanoId = "ciudadano-demo";
        private String nombreCiudadano = "Martina López";

        Rol get() { return rol; }
        Actor actual() { return new Actor(rol, ciudadanoId, nombreCiudadano, organizacionId); }
        void autenticar(String ciudadanoId, String nombre, Rol rol, String organizacionId) {
            this.ciudadanoId = ciudadanoId;
            this.nombreCiudadano = nombre;
            this.rol = rol;
            this.organizacionId = organizacionId;
        }
        void cambiar(Rol rol) { this.rol = rol; }
        void elegirOrganizacion(String organizacionId) { this.organizacionId = organizacionId; }
    }

    // ======================================================================
    // cuenta/
    // ======================================================================

    // cuenta/Telefono.java: normalización exacta de celulares UY/BR
    static final class Telefono {
        private static final Pattern FORMATO = Pattern.compile("\\+?[0-9 ().\\-]+");
        private static final Pattern CELULAR_UY = Pattern.compile("5989\\d{7}");
        private static final Pattern CELULAR_BR = Pattern.compile("55[1-9]{2}9\\d{8}");

        static String normalizar(String numero, Pais pais) {
            if (numero == null || pais == null) {
                throw new IllegalArgumentException("teléfono o país vacío");
            }
            var limpio = numero.strip();
            if (!FORMATO.matcher(limpio).matches()) {
                throw new IllegalArgumentException("caracteres inválidos en el teléfono: " + numero);
            }
            var digitos = limpio.replaceAll("\\D", "");
            String completo;
            if (limpio.startsWith("+")) {
                completo = digitos;
            } else if (digitos.startsWith("00")) {
                completo = digitos.substring(2);
            } else if (digitos.startsWith(pais.codigo()) && digitos.length() == largoCompleto(pais)) {
                completo = digitos;
            } else {
                completo = pais.codigo() + sinCeroInicial(digitos);
            }
            if (!CELULAR_UY.matcher(completo).matches()
                    && !CELULAR_BR.matcher(completo).matches()) {
                throw new IllegalArgumentException("no es un celular de Uruguay ni de Brasil: " + numero);
            }
            return completo;
        }

        static String presentar(String normalizado) {
            if (CELULAR_UY.matcher(normalizado).matches()) {
                var n = normalizado.substring(3);
                return "+598 " + n.substring(0, 2) + " " + n.substring(2, 5)
                        + " " + n.substring(5);
            }
            if (CELULAR_BR.matcher(normalizado).matches()) {
                var n = normalizado.substring(2);
                return "+55 " + n.substring(0, 2) + " " + n.substring(2, 7)
                        + "-" + n.substring(7);
            }
            return "+" + normalizado;
        }

        private static int largoCompleto(Pais pais) {
            return pais == Pais.UY ? 11 : 13;
        }

        private static String sinCeroInicial(String digitos) {
            return digitos.startsWith("0") ? digitos.substring(1) : digitos;
        }
    }

    // cuenta/CuentaException.java
    static class CuentaException extends RuntimeException {
        private final String campo;
        CuentaException(String campo, String mensaje) {
            super(mensaje);
            this.campo = campo;
        }
        String campo() { return campo; }
    }

    // Sustituto del BCryptPasswordEncoder: sal aleatoria + SHA-256.
    // Mismo contrato: encode() nunca repite salida, matches() verifica.
    static final class Cifrado {
        private static final SecureRandom AZAR = new SecureRandom();

        static String encode(String clave) {
            var sal = new byte[12];
            AZAR.nextBytes(sal);
            var salHex = hex(sal);
            return salHex + ":" + hash(salHex + clave);
        }

        static boolean matches(String clave, String codificado) {
            if (codificado == null || !codificado.contains(":")) {
                return false;
            }
            var sal = codificado.substring(0, codificado.indexOf(':'));
            return MessageDigest.isEqual(
                    hash(sal + clave).getBytes(StandardCharsets.UTF_8),
                    codificado.substring(sal.length() + 1).getBytes(StandardCharsets.UTF_8));
        }

        private static String hash(String texto) {
            try {
                return hex(MessageDigest.getInstance("SHA-256")
                        .digest(texto.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private static String hex(byte[] bytes) {
            var sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16))
                        .append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        }
    }

    // cuenta/Usuario.java
    record Usuario(String id, String telefono, String claveHash, Rol rol,
            String organizacionId) { }

    // cuenta/interno/UsuarioEntidad.java
    static class UsuarioEntidad {
        private final String id;
        private final String telefono;
        private String claveHash;
        private final Rol rol;
        private final String organizacionId;
        private final String nombre;

        UsuarioEntidad(String id, String telefono, String claveHash, Rol rol,
                String organizacionId, String nombre) {
            this.id = id;
            this.telefono = telefono;
            this.claveHash = claveHash;
            this.rol = rol;
            this.organizacionId = organizacionId;
            this.nombre = nombre;
        }
        String getId() { return id; }
        String getTelefono() { return telefono; }
        String getClaveHash() { return claveHash; }
        Rol getRol() { return rol; }
        String getOrganizacionId() { return organizacionId; }
        String getNombre() { return nombre; }
        void renovarClaveHash(String claveHash) { this.claveHash = claveHash; }
    }

    // cuenta/interno/UsuariosRepository.java -> almacén en memoria
    static class UsuariosRepo {
        private final Map<String, UsuarioEntidad> porId = new LinkedHashMap<>();
        void save(UsuarioEntidad u) { porId.put(u.getId(), u); }
        UsuarioEntidad porId(String id) { return porId.get(id); }
        Optional<UsuarioEntidad> findByNombre(String nombre) {
            return porId.values().stream()
                    .filter(u -> u.getNombre().equals(nombre)).findFirst();
        }
        Optional<UsuarioEntidad> findByTelefono(String telefono) {
            return porId.values().stream()
                    .filter(u -> u.getTelefono().equals(telefono)).findFirst();
        }
    }

    // cuenta/interno/UsuarioPrincipal.java
    static class UsuarioPrincipal {
        private final UsuarioEntidad u;
        UsuarioPrincipal(UsuarioEntidad u) { this.u = u; }
        String getCiudadanoId() { return u.getId(); }
        String getNombre() { return u.getNombre(); }
        Rol getRol() { return u.getRol(); }
        String getOrganizacionId() { return u.getOrganizacionId(); }
        String getPassword() { return u.getClaveHash(); }
    }

    // cuenta/Cuentas.java: reglas de registro y entrada, idénticas
    static class Cuentas {
        static final int CLAVE_MINIMA = 5;
        static final int CLAVE_MAXIMA_BYTES = 72;
        private static final int NOMBRE_MINIMO = 5;
        private static final Pattern NOMBRE_FORMATO =
                Pattern.compile("[a-zA-Z0-9_.\\-]+");

        private final UsuariosRepo usuarios;
        private final String hashSinCuenta;

        Cuentas(UsuariosRepo usuarios) {
            this.usuarios = usuarios;
            this.hashSinCuenta = Cifrado.encode(UUID.randomUUID().toString());
        }

        Usuario registrarCiudadano(String nombre, String numero, Pais pais, String clave) {
            String telefono = validarDatos(nombre, numero, pais, clave);
            var usuario = new Usuario(UUID.randomUUID().toString(), telefono,
                    Cifrado.encode(clave), Rol.CIUDADANO, null);
            usuarios.save(new UsuarioEntidad(usuario.id(), telefono, usuario.claveHash(),
                    usuario.rol(), null, nombre.strip().toLowerCase()));
            return usuario;
        }

        String validarDatos(String nombre, String numero, Pais pais, String clave) {
            if (nombre == null || nombre.strip().length() < NOMBRE_MINIMO) {
                throw new CuentaException("nombre",
                        "El nombre de usuario necesita al menos 5 caracteres");
            }
            var usuario = nombre.strip().toLowerCase();
            if (!NOMBRE_FORMATO.matcher(usuario).matches()) {
                throw new CuentaException("nombre",
                        "El nombre de usuario no admite espacios ni símbolos");
            }
            String telefono;
            try {
                telefono = Telefono.normalizar(numero, pais);
            } catch (IllegalArgumentException e) {
                throw new CuentaException("telefono", "Ingresá un celular de Uruguay o Brasil");
            }
            if (clave == null || clave.length() < CLAVE_MINIMA
                    || clave.getBytes(StandardCharsets.UTF_8).length > CLAVE_MAXIMA_BYTES) {
                throw new CuentaException("clave",
                        "La contraseña necesita al menos 5 caracteres");
            }
            if (usuarios.findByNombre(usuario).isPresent()) {
                throw new CuentaException("nombre", "Ese nombre de usuario ya está en uso");
            }
            if (usuarios.findByTelefono(telefono).isPresent()) {
                throw new CuentaException("telefono", "Ese celular ya tiene una cuenta");
            }
            return telefono;
        }

        Optional<String> telefonoDe(String usuarioId) {
            return usuarioId == null ? Optional.empty()
                    : Optional.ofNullable(usuarios.porId(usuarioId))
                            .map(u -> Telefono.presentar(u.getTelefono()));
        }

        Optional<Usuario> buscar(String numero, Pais pais) {
            try {
                return usuarios.findByTelefono(Telefono.normalizar(numero, pais))
                        .map(u -> new Usuario(u.getId(), u.getTelefono(), u.getClaveHash(),
                                u.getRol(), u.getOrganizacionId()));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }

        Optional<Usuario> entrar(String numero, Pais pais, String clave) {
            var usuario = buscar(numero, pais);
            var hash = usuario.map(Usuario::claveHash).orElse(hashSinCuenta);
            boolean coincide = clave != null && Cifrado.matches(clave, hash);
            return coincide ? usuario : Optional.empty();
        }
    }

    // ModoIntentosFiltro.java: máx. N envíos del login por dirección y minuto
    static class Intentos {
        private static final int DIRECCIONES = 1024;
        private static final Duration VENTANA = Duration.ofMinutes(1);

        private record Intento(Instant vence, int cantidad) { }

        private final Map<String, Intento> intentos = new HashMap<>();
        private final int maximo;

        Intentos(int maximo) {
            if (maximo < 1) {
                throw new IllegalArgumentException("El limite de intentos debe ser positivo");
            }
            this.maximo = maximo;
        }

        synchronized boolean permitir(String direccion) {
            var ahora = Instant.now();
            intentos.entrySet().removeIf(e -> !ahora.isBefore(e.getValue().vence()));
            var intento = intentos.get(direccion);
            if (intento == null) {
                if (intentos.size() >= DIRECCIONES) {
                    return false;
                }
                intentos.put(direccion, new Intento(ahora.plus(VENTANA), 1));
                return true;
            }
            if (intento.cantidad() >= maximo) {
                return false;
            }
            intentos.put(direccion, new Intento(intento.vence(), intento.cantidad() + 1));
            return true;
        }
    }

    // ======================================================================
    // shared/ catálogo de materiales
    // ======================================================================

    // shared/Material.java: igualdad por código (la clave de negocio)
    static class Material {
        private static long secuencia = 0;
        private final Long id;
        private final String codigo;
        private final String nombre;
        private final String icono;
        private boolean activo;

        Material(String codigo, String nombre, String icono) {
            this.id = ++secuencia;
            this.codigo = codigo;
            this.nombre = nombre;
            this.icono = icono;
            this.activo = true;
        }
        Long getId() { return id; }
        String getCodigo() { return codigo; }
        String getNombre() { return nombre; }
        boolean isActivo() { return activo; }
        void darDeBaja() { activo = false; }
        @Override public boolean equals(Object o) {
            return o instanceof Material m && Objects.equals(codigo, m.codigo);
        }
        @Override public int hashCode() { return Objects.hashCode(codigo); }
        @Override public String toString() { return codigo; }
    }

    // shared/CatalogoMateriales.java
    static class CatalogoMateriales {
        private final List<Material> repositorio = new ArrayList<>();

        void semillarFaltantes() {
            List.of(
                    new String[] {"PLASTICO", "Plástico", "botella"},
                    new String[] {"CARTON", "Cartón", "caja"},
                    new String[] {"PAPEL", "Papel", "hoja"},
                    new String[] {"VIDRIO", "Vidrio", "frasco"},
                    new String[] {"METAL", "Metal", "lata"},
                    new String[] {"ELECTRONICOS", "Electrónicos", "chip"},
                    new String[] {"LATAS", "Latas", "lata"}
            ).forEach(d -> {
                if (repositorio.stream().noneMatch(m -> m.getCodigo().equals(d[0]))) {
                    repositorio.add(new Material(d[0], d[1], d[2]));
                }
            });
        }

        List<Material> activos() {
            return repositorio.stream().filter(Material::isActivo)
                    .sorted(Comparator.comparing(Material::getId)).toList();
        }
        List<Material> todos() { return List.copyOf(repositorio); }
        Material resolver(String codigo) {
            return repositorio.stream().filter(m -> m.getCodigo().equals(codigo))
                    .findFirst().orElseThrow(() ->
                            new IllegalArgumentException("Material desconocido: " + codigo));
        }
        List<Material> seleccionActiva(java.util.Collection<Material> pedidos) {
            var resueltos = pedidos.stream().map(m -> resolver(m.getCodigo())).toList();
            if (resueltos.stream().anyMatch(m -> !m.isActivo())) {
                throw new IllegalArgumentException("Material inactivo seleccionado");
            }
            return resueltos;
        }
    }

    // ======================================================================
    // organizacion/
    // ======================================================================

    // organizacion/interno/Organizacion.java
    static class Organizacion {
        static final String PATRON_TELEFONO =
                "^(?=(?:\\D*\\d){8,})\\+?[0-9(][0-9 ()\\-.]{4,"
                        + (Limites.TELEFONO - 3) + "}[0-9]$";
        static final String PATRON_TELEFONO_O_VACIO = "^\\s*$|" + PATRON_TELEFONO;

        private final String id;
        private final String nombre;
        private final Ciudad ciudad;
        private List<Material> materiales;
        private String horario;
        private String telefono;
        private Double latitud;
        private Double longitud;
        private long version;

        Organizacion(String id, String nombre, Ciudad ciudad,
                List<Material> materiales, String horario, String telefono,
                Ubicacion ubicacion) {
            this.id = id;
            this.nombre = nombre;
            this.ciudad = ciudad;
            this.materiales = new ArrayList<>(materiales);
            this.horario = horario;
            this.telefono = telefono;
            if (ubicacion != null) {
                this.latitud = ubicacion.latitud();
                this.longitud = ubicacion.longitud();
            }
        }
        String getId() { return id; }
        String getNombre() { return nombre; }
        Ciudad getCiudad() { return ciudad; }
        List<Material> getMateriales() { return materiales; }
        String getHorario() { return horario; }
        String getTelefono() { return telefono; }
        long getVersion() { return version; }
        Ubicacion ubicacion() {
            return latitud == null || longitud == null ? null : new Ubicacion(latitud, longitud);
        }
        void actualizar(List<Material> materiales, String horario, String telefono,
                Ubicacion ubicacion) {
            this.materiales = new ArrayList<>(materiales);
            this.horario = horario;
            this.telefono = telefono;
            this.latitud = ubicacion == null ? null : ubicacion.latitud();
            this.longitud = ubicacion == null ? null : ubicacion.longitud();
        }
        void incrementarVersion() { version++; }
    }

    // organizacion/OrganizacionInfo.java
    record OrganizacionInfo(String id, String nombre, Ciudad ciudad,
            List<Material> materiales, String horario, String telefono,
            Ubicacion ubicacion, long version) {
        // RN-04: solo cuentan los materiales activos del catálogo
        boolean recibeTodos(java.util.Collection<Material> pedidos) {
            return materiales.stream().filter(Material::isActivo)
                    .collect(toSet()).containsAll(pedidos);
        }
    }

    // organizacion/AcopiosCiudad.java
    record AcopiosCiudad(Ciudad ciudad, List<OrganizacionInfo> organizaciones) { }

    // organizacion/OrganizacionService.java (sin Memo/AlmacenDelPedido:
    // es una optimización por pedido, no cambia el resultado)
    static class OrganizacionService {
        private final Map<String, Organizacion> repositorio = new LinkedHashMap<>();
        private final CatalogoMateriales catalogo;

        OrganizacionService(CatalogoMateriales catalogo) {
            this.catalogo = catalogo;
        }

        List<OrganizacionInfo> todas() {
            return repositorio.values().stream()
                    .sorted(Comparator.comparing(Organizacion::getNombre))
                    .map(OrganizacionService::aInfo).toList();
        }

        List<AcopiosCiudad> porCiudad() {
            var agrupadas = todas().stream().collect(groupingBy(OrganizacionInfo::ciudad,
                    () -> new EnumMap<>(Ciudad.class), toList()));
            return Arrays.stream(Ciudad.values())
                    .map(c -> new AcopiosCiudad(c, agrupadas.getOrDefault(c, List.of())))
                    .toList();
        }

        // sin organización válida en sesión: cae en la primera por id
        OrganizacionInfo actual(Actor actor) {
            String id = actor.organizacionId();
            var encontrada = id != null ? buscar(id) : Optional.<OrganizacionInfo>empty();
            return encontrada.orElseGet(this::primera);
        }

        Optional<OrganizacionInfo> buscar(String id) {
            return Optional.ofNullable(repositorio.get(id)).map(OrganizacionService::aInfo);
        }

        List<OrganizacionInfo> compatibles(Ciudad ciudad,
                java.util.Collection<Material> materiales) {
            return enCiudad(ciudad).stream().filter(o -> o.recibeTodos(materiales)).toList();
        }

        List<OrganizacionInfo> enCiudad(Ciudad ciudad) {
            return repositorio.values().stream()
                    .filter(o -> o.getCiudad() == ciudad)
                    .sorted(Comparator.comparing(Organizacion::getNombre))
                    .map(OrganizacionService::aInfo).toList();
        }

        Map<String, String> nombres(java.util.Collection<String> ids) {
            var nombres = new HashMap<String, String>();
            ids.forEach(id -> Optional.ofNullable(repositorio.get(id))
                    .ifPresent(o -> nombres.put(o.getId(), o.getNombre())));
            return nombres;
        }

        boolean actualizarPerfil(String id, List<Material> materiales,
                String horario, String telefono, Ubicacion ubicacion, long version) {
            var org = repositorio.get(id);
            if (org.getVersion() != version) {
                return false;
            }
            org.actualizar(catalogo.seleccionActiva(materiales), horario, telefono, ubicacion);
            org.incrementarVersion(); // @Version al hacer flush
            return true;
        }

        void guardar(Organizacion o) { repositorio.put(o.getId(), o); }

        private OrganizacionInfo primera() {
            return repositorio.keySet().stream().sorted().findFirst()
                    .map(repositorio::get).map(OrganizacionService::aInfo).orElseThrow();
        }

        private static OrganizacionInfo aInfo(Organizacion o) {
            return new OrganizacionInfo(o.getId(), o.getNombre(), o.getCiudad(),
                    List.copyOf(o.getMateriales()), o.getHorario(), o.getTelefono(),
                    o.ubicacion(), o.getVersion());
        }
    }

    // ======================================================================
    // solicitud/
    // ======================================================================

    // i18n: la consola resuelve las claves contra el messages.properties REAL
    // del proyecto. Si falta una traducción, la clave queda cruda — el mismo
    // síntoma que vería un usuario en la app.
    static final class Mensajes {
        private static final java.util.Properties P = new java.util.Properties();
        static {
            var ruta = java.nio.file.Path.of("src/main/resources/messages.properties");
            if (!java.nio.file.Files.exists(ruta)) {
                ruta = java.nio.file.Path.of(
                        "../src/main/resources/messages.properties");
            }
            try (var rd = java.nio.file.Files.newBufferedReader(ruta,
                    StandardCharsets.UTF_8)) {
                P.load(rd); // Spring Boot lee messages en UTF-8 por defecto
            } catch (Exception e) {
                // sin archivo: las claves se muestran crudas
            }
        }
        static String t(String clave, Object... args) {
            var v = P.getProperty(clave);
            if (v == null) {
                return clave;
            }
            return java.text.MessageFormat.format(v, args);
        }
    }

    // solicitud/Estado.java
    enum Estado {
        PENDIENTE("estado.pendiente"),
        EN_CURSO("estado.en-curso"),
        COMPLETADA("estado.completada"),
        RECHAZADA("estado.rechazada"),
        CANCELADA("estado.cancelada");

        private final String clave;
        Estado(String clave) { this.clave = clave; }
        String getClave() { return clave; }
        String etiqueta() { return Mensajes.t(clave); }

        boolean esFinal() {
            return this == COMPLETADA || this == RECHAZADA || this == CANCELADA;
        }

        private static final Set<Estado> FINALES = EnumSet.allOf(Estado.class).stream()
                .filter(Estado::esFinal).collect(Collectors.toUnmodifiableSet());
        static Set<Estado> finales() { return FINALES; }

        boolean permiteAceptar() { return this == PENDIENTE; }
        boolean permiteRechazar() { return this == PENDIENTE || this == EN_CURSO; }
        boolean permiteCompletar() { return this == EN_CURSO; }
        boolean permiteCancelar() { return this == PENDIENTE; }
    }

    // solicitud/TipoAviso.java
    enum TipoAviso {
        NUEVA("aviso.nueva", Estado.PENDIENTE),
        ACEPTADA("aviso.aceptada", Estado.EN_CURSO),
        RECHAZADA("aviso.rechazada", Estado.RECHAZADA),
        COMPLETADA("aviso.completada", Estado.COMPLETADA),
        CANCELADA("aviso.cancelada", Estado.CANCELADA);

        private final String clave;
        private final Estado estado;
        TipoAviso(String clave, Estado estado) {
            this.clave = clave;
            this.estado = estado;
        }
        String getClave() { return clave; }
        Estado getEstado() { return estado; }
        boolean esParaCiudadano() {
            return this == ACEPTADA || this == RECHAZADA || this == COMPLETADA;
        }
    }

    // solicitud/Resultado.java
    enum Resultado { OK, NO_ENCONTRADA, SIN_PERMISO, CONFLICTO, INVALIDA }

    // solicitud/Filtro.java
    enum Filtro {
        TODAS, ABIERTAS, CERRADAS;
        boolean muestra(Estado estado) {
            return switch (this) {
                case TODAS -> true;
                case ABIERTAS -> !estado.esFinal();
                case CERRADAS -> estado.esFinal();
            };
        }
        Set<Estado> estados() {
            return EnumSet.allOf(Estado.class).stream().filter(this::muestra)
                    .collect(Collectors.toUnmodifiableSet());
        }
        static Filtro desde(String valor) {
            if (valor == null) {
                return TODAS;
            }
            try {
                return valueOf(valor.toUpperCase());
            } catch (IllegalArgumentException e) {
                return TODAS;
            }
        }
    }

    // solicitud/TransicionInvalidaException.java
    static class TransicionInvalidaException extends RuntimeException {
        TransicionInvalidaException(Estado estado, String accion) {
            super(estado + " no admite " + accion);
        }
    }

    // solicitud/interno/Ciudadano.java
    static class Ciudadano {
        private final String id;
        private String nombre;
        Ciudadano(String id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
        String getId() { return id; }
        String getNombre() { return nombre; }
        void renombrar(String nombre) { this.nombre = nombre; }
    }

    // solicitud/interno/Solicitud.java: máquina de estados + @Version
    static class Solicitud {
        private Long id;
        private Ciudadano ciudadano;
        private String direccion;
        private String referencia;
        private Double latitud;
        private Double longitud;
        private List<Material> materiales = new ArrayList<>();
        private String organizacionId;
        private String contacto;
        private String nota;
        private Estado estado;
        private Instant creadaEn;
        private Instant respondidaEn;
        private Instant aceptadaEn;
        private Instant finalizadaEn;
        private long version;

        static Solicitud nueva(String direccion, String referencia, Ubicacion ubicacion,
                List<Material> materiales, String organizacionId, String nota,
                String contacto, Ciudadano ciudadano) {
            var sol = new Solicitud();
            sol.ciudadano = ciudadano;
            sol.direccion = direccion;
            sol.referencia = referencia;
            sol.ponerUbicacion(ubicacion);
            sol.materiales = new ArrayList<>(materiales);
            sol.organizacionId = organizacionId;
            sol.contacto = contacto;
            sol.nota = nota;
            sol.estado = Estado.PENDIENTE;
            sol.creadaEn = Instant.now();
            return sol;
        }

        private Solicitud() { }

        static Solicitud semilla(SolicitudSemilla s, Ciudadano ciudadano) {
            if (s.estado().esFinal() != (s.finalizadaEn() != null)) {
                throw new IllegalArgumentException("finalizadaEn inconsistente con el estado");
            }
            if (s.finalizadaEn() != null && s.finalizadaEn().isBefore(s.creadaEn())) {
                throw new IllegalArgumentException("finalizadaEn anterior a creadaEn");
            }
            if (s.respondidaEn() != null && s.estado() == Estado.PENDIENTE) {
                throw new IllegalArgumentException("respondidaEn en una pendiente");
            }
            if (s.respondidaEn() != null && s.respondidaEn().isBefore(s.creadaEn())) {
                throw new IllegalArgumentException("respondidaEn anterior a creadaEn");
            }
            var sol = new Solicitud();
            sol.ciudadano = ciudadano;
            sol.direccion = s.direccion();
            sol.referencia = s.referencia();
            sol.ponerUbicacion(s.ubicacion());
            sol.materiales = new ArrayList<>(s.materiales());
            sol.organizacionId = s.organizacionId();
            sol.contacto = s.contacto();
            sol.nota = s.nota();
            sol.estado = s.estado();
            sol.creadaEn = s.creadaEn();
            sol.respondidaEn = s.respondidaEn();
            sol.finalizadaEn = s.finalizadaEn();
            sol.aceptadaEn = aceptadaSegun(s.estado(), s.respondidaEn(), s.finalizadaEn());
            return sol;
        }

        private static Instant aceptadaSegun(Estado estado, Instant respondidaEn,
                Instant finalizadaEn) {
            if (respondidaEn == null) {
                return null;
            }
            return switch (estado) {
                case EN_CURSO, COMPLETADA -> respondidaEn;
                case RECHAZADA -> respondidaEn.isBefore(finalizadaEn) ? respondidaEn : null;
                default -> null;
            };
        }

        void aceptar(Instant ahora) {
            exigir(estado.permiteAceptar(), "aceptar");
            estado = Estado.EN_CURSO;
            respondidaEn = ahora;
            aceptadaEn = ahora;
        }

        void rechazar(Instant ahora) {
            exigir(estado.permiteRechazar(), "rechazar");
            if (respondidaEn == null) {
                respondidaEn = ahora;
            }
            cerrar(Estado.RECHAZADA, ahora);
        }

        void completar(Instant ahora) {
            exigir(estado.permiteCompletar(), "completar");
            cerrar(Estado.COMPLETADA, ahora);
        }

        void cancelar(Instant ahora) {
            exigir(estado.permiteCancelar(), "cancelar");
            cerrar(Estado.CANCELADA, ahora);
        }

        Ubicacion ubicacion() {
            return latitud == null || longitud == null ? null
                    : new Ubicacion(latitud, longitud);
        }

        private void ponerUbicacion(Ubicacion u) {
            if (u != null) {
                latitud = u.latitud();
                longitud = u.longitud();
            }
        }

        private void exigir(boolean permitido, String accion) {
            if (!permitido) {
                throw new TransicionInvalidaException(estado, accion);
            }
        }

        private void cerrar(Estado destino, Instant ahora) {
            estado = destino;
            finalizadaEn = ahora;
        }

        Long getId() { return id; }
        void fijarId(long id) { this.id = id; }
        Ciudadano getCiudadano() { return ciudadano; }
        String getDireccion() { return direccion; }
        String getReferencia() { return referencia; }
        List<Material> getMateriales() { return materiales; }
        String getOrganizacionId() { return organizacionId; }
        String getContacto() { return contacto; }
        String getNota() { return nota; }
        Estado getEstado() { return estado; }
        Instant getCreadaEn() { return creadaEn; }
        Instant getRespondidaEn() { return respondidaEn; }
        Instant getAceptadaEn() { return aceptadaEn; }
        Instant getFinalizadaEn() { return finalizadaEn; }
        long getVersion() { return version; }
        void incrementarVersion() { version++; }
    }

    // solicitud/SolicitudSemilla.java
    record SolicitudSemilla(String ciudadanoId, String nombreCiudadano,
            String contacto, String direccion, String referencia, Ubicacion ubicacion,
            List<Material> materiales, String organizacionId, String nota, Estado estado,
            Instant creadaEn, Instant respondidaEn, Instant finalizadaEn) { }

    // solicitud/SolicitudCreacion.java: valida igual que el record real
    record SolicitudCreacion(Ciudad ciudad, String direccion, String referencia,
            Ubicacion ubicacion, List<Material> materiales, String organizacionId,
            String nombre, String contacto, String nota) {
        SolicitudCreacion {
            if (ciudad == null
                    || direccion == null || direccion.isBlank() || direccion.length() > 120
                    || referencia != null && referencia.length() > 120
                    || materiales == null || materiales.isEmpty()
                    || organizacionId == null || organizacionId.isBlank()
                    || nombre == null || nombre.isBlank() || nombre.length() > 80
                    || contacto == null || contacto.isBlank() || contacto.length() > 40
                    || nota != null && nota.length() > 300) {
                throw new IllegalArgumentException("SolicitudCreacion inválida");
            }
            materiales = List.copyOf(materiales);
        }
    }

    // solicitud/SolicitudInfo.java
    record SolicitudInfo(long id, String direccion, String referencia,
            Ubicacion ubicacion, List<Material> materiales, String organizacionNombre,
            String nombreCiudadano, String contacto, String nota, Estado estado,
            Instant creadaEn, Instant finalizadaEn, long version) {

        static SolicitudInfo desde(Solicitud s, Map<String, String> nombres) {
            return new SolicitudInfo(s.getId(), s.getDireccion(), s.getReferencia(),
                    s.ubicacion(), List.copyOf(s.getMateriales()),
                    nombres.getOrDefault(s.getOrganizacionId(), s.getOrganizacionId()),
                    s.getCiudadano().getNombre(), s.getContacto(), s.getNota(),
                    s.getEstado(), s.getCreadaEn(), s.getFinalizadaEn(), s.getVersion());
        }

        boolean contactoEsTelefono() {
            return contacto != null && contacto.matches("^[+0-9 ().-]{6,}$");
        }
        boolean tieneDetalle() {
            return ubicacion != null || (nota != null && !nota.isBlank());
        }
        String creada() { return Fechas.corta(creadaEn); }
        String finalizada() { return Fechas.corta(finalizadaEn); }
    }

    // solicitud/Propiedad.java
    static final class Propiedad {
        static boolean esAutor(Actor actor, String ciudadanoId) {
            return actor.esCiudadano() && ciudadanoId.equals(actor.ciudadanoId());
        }
        static boolean esDestinataria(Actor actor, String organizacionId) {
            return actor.esOrganizacion() && organizacionId.equals(actor.organizacionId());
        }
    }

    // solicitud/interno/Aviso.java
    static class Aviso {
        private Long id;
        private final String destino;
        private final Long solicitudId;
        private final TipoAviso tipo;
        private final String detalle;
        private boolean leida;
        private final Instant creadaEn;

        Aviso(String destino, Long solicitudId, TipoAviso tipo, String detalle,
                Instant creadaEn) {
            this.destino = destino;
            this.solicitudId = solicitudId;
            this.tipo = tipo;
            this.detalle = detalle;
            this.creadaEn = creadaEn;
        }
        Long getId() { return id; }
        void fijarId(long id) { this.id = id; }
        String getDestino() { return destino; }
        Long getSolicitudId() { return solicitudId; }
        TipoAviso getTipo() { return tipo; }
        String getDetalle() { return detalle; }
        boolean isLeida() { return leida; }
        Instant getCreadaEn() { return creadaEn; }
        void marcarLeida() { leida = true; }
    }

    // solicitud/AvisoSemilla.java
    record AvisoSemilla(String destino, Long solicitudId, TipoAviso tipo,
            String detalle, boolean leida, Instant creadaEn) { }

    // solicitud/AvisoInfo.java
    record AvisoInfo(long id, TipoAviso tipo, String detalle, Long solicitudId,
            boolean leida, Instant creadaEn, Estado estado) { }

    // solicitud/GrupoSolicitudes.java, BandejaSolicitudes.java, ColumnaAvisos.java
    record GrupoSolicitudes(String clave, List<SolicitudInfo> solicitudes, long total) { }
    record BandejaSolicitudes(List<GrupoSolicitudes> grupos) { }
    record ColumnaAvisos(String clave, List<AvisoInfo> avisos) { }

    // solicitud/SolicitudService.java
    static class SolicitudService {
        private final Map<Long, Solicitud> repositorio = new LinkedHashMap<>();
        private final Map<String, Ciudadano> ciudadanos = new LinkedHashMap<>();
        private long secuencia = 0;
        private final AvisoService avisos;
        private final OrganizacionService organizaciones;
        private final CatalogoMateriales catalogo;

        SolicitudService(AvisoService avisos, OrganizacionService organizaciones,
                CatalogoMateriales catalogo) {
            this.avisos = avisos;
            this.organizaciones = organizaciones;
            this.catalogo = catalogo;
        }

        // Optional.empty() = el centro no recibe esos materiales en esa ciudad (RN-04)
        Optional<SolicitudInfo> crear(Actor actor, SolicitudCreacion datos) {
            boolean compatible = organizaciones.compatibles(datos.ciudad(), datos.materiales())
                    .stream().anyMatch(o -> o.id().equals(datos.organizacionId()));
            if (!compatible) {
                return Optional.empty();
            }
            var materiales = catalogo.seleccionActiva(datos.materiales());
            var s = Solicitud.nueva(datos.direccion(), datos.referencia(), datos.ubicacion(),
                    materiales, datos.organizacionId(), datos.nota(), datos.contacto(),
                    ciudadano(actor.ciudadanoId(), datos.nombre()));
            save(s);
            avisos.nueva(s);
            String nombreOrg = organizaciones.buscar(datos.organizacionId())
                    .map(OrganizacionInfo::nombre).orElse(datos.organizacionId());
            return Optional.of(SolicitudInfo.desde(s, Map.of(datos.organizacionId(), nombreOrg)));
        }

        Resultado aceptar(Actor actor, long id, long version) {
            return aplicar(actor, id, version, s -> esDestinataria(actor, s),
                    s -> s.aceptar(Instant.now()), TipoAviso.ACEPTADA);
        }
        Resultado rechazar(Actor actor, long id, long version) {
            return aplicar(actor, id, version, s -> esDestinataria(actor, s),
                    s -> s.rechazar(Instant.now()), TipoAviso.RECHAZADA);
        }
        Resultado completar(Actor actor, long id, long version) {
            return aplicar(actor, id, version, s -> esDestinataria(actor, s),
                    s -> s.completar(Instant.now()), TipoAviso.COMPLETADA);
        }
        Resultado cancelar(Actor actor, long id, long version) {
            return aplicar(actor, id, version, s -> esAutor(actor, s),
                    s -> s.cancelar(Instant.now()), TipoAviso.CANCELADA);
        }

        Optional<SolicitudInfo> detalle(Actor actor, long id) {
            return Optional.ofNullable(repositorio.get(id))
                    .filter(s -> esDestinataria(actor, s) || esAutor(actor, s))
                    .map(s -> aInfos(List.of(s)).get(0));
        }

        List<SolicitudInfo> todas() {
            var todas = repositorio.values().stream()
                    .sorted(Comparator.comparing(Solicitud::getId)).toList();
            return aInfos(todas);
        }

        boolean vacia() { return repositorio.isEmpty(); }

        List<Solicitud> buscar(Predicate<Solicitud> filtro,
                Comparator<Solicitud> orden, Integer tope) {
            var flujo = repositorio.values().stream().filter(filtro).sorted(orden);
            var lista = flujo.toList();
            return tope == null ? lista : lista.subList(0, Math.min(tope, lista.size()));
        }
        long contar(Predicate<Solicitud> filtro) {
            return repositorio.values().stream().filter(filtro).count();
        }
        List<Solicitud> todasEntidades() {
            return List.copyOf(repositorio.values());
        }

        void save(Solicitud s) {
            if (s.getId() == null) {
                s.fijarId(++secuencia);
            }
            repositorio.put(s.getId(), s);
        }

        // alta o renombre de ciudadano, igual que ciudadano() en la app
        Ciudadano ciudadanoSemilla(String id, String nombre) {
            return ciudadano(id, nombre);
        }

        private Ciudadano ciudadano(String id, String nombre) {
            var existente = ciudadanos.get(id);
            if (existente != null) {
                if (!existente.getNombre().equals(nombre)) {
                    existente.renombrar(nombre);
                }
                return existente;
            }
            var nuevo = new Ciudadano(id, nombre);
            ciudadanos.put(id, nuevo);
            return nuevo;
        }

        private Resultado aplicar(Actor actor, long id, long version,
                Predicate<Solicitud> permiso, Consumer<Solicitud> transicion,
                TipoAviso tipoAviso) {
            var s = repositorio.get(id);
            if (s == null) {
                return Resultado.NO_ENCONTRADA;
            }
            if (!permiso.test(s)) {
                return Resultado.SIN_PERMISO;
            }
            if (s.getVersion() != version) {
                return Resultado.CONFLICTO;
            }
            try {
                transicion.accept(s);
                s.incrementarVersion(); // @Version al hacer saveAndFlush
                avisos.avisar(s, tipoAviso);
                return Resultado.OK;
            } catch (TransicionInvalidaException e) {
                return Resultado.INVALIDA;
            }
        }

        private boolean esDestinataria(Actor actor, Solicitud s) {
            return actor.esOrganizacion()
                    && s.getOrganizacionId().equals(organizaciones.actual(actor).id());
        }
        private boolean esAutor(Actor actor, Solicitud s) {
            return Propiedad.esAutor(actor, s.getCiudadano().getId());
        }

        List<SolicitudInfo> aInfos(List<Solicitud> solicitudes) {
            var nombres = organizaciones.nombres(solicitudes.stream()
                    .map(Solicitud::getOrganizacionId).collect(toSet()));
            return solicitudes.stream().map(s -> SolicitudInfo.desde(s, nombres)).toList();
        }
    }

    // solicitud/AvisoService.java
    static class AvisoService {
        private final Map<Long, Aviso> repositorio = new LinkedHashMap<>();
        private long secuencia = 0;
        private SolicitudService solicitudes;
        private OrganizacionService organizaciones;

        void vincular(SolicitudService solicitudes, OrganizacionService organizaciones) {
            this.solicitudes = solicitudes;
            this.organizaciones = organizaciones;
        }

        List<AvisoInfo> avisosPara(Actor actor) {
            var lista = repositorio.values().stream()
                    .filter(a -> a.getDestino().equals(destino(actor)))
                    .sorted(Comparator.comparing(Aviso::getCreadaEn).reversed()).toList();
            var estados = estadosDe(lista);
            return lista.stream().map(a -> aInfo(a, estados)).toList();
        }

        List<ColumnaAvisos> kanbanPara(Actor actor) {
            var lista = avisosPara(actor);
            return List.of(
                    new ColumnaAvisos("org.grupo.pendientes",
                            lista.stream().filter(a -> a.estado() == Estado.PENDIENTE).toList()),
                    new ColumnaAvisos("org.grupo.en-curso",
                            lista.stream().filter(a -> a.estado() == Estado.EN_CURSO).toList()),
                    new ColumnaAvisos("org.grupo.cerradas",
                            lista.stream().filter(a -> a.estado().esFinal()).toList()));
        }

        long avisosSinLeer(Actor actor) {
            return repositorio.values().stream()
                    .filter(a -> a.getDestino().equals(destino(actor)) && !a.isLeida()).count();
        }

        void marcarLeidas(Actor actor) {
            repositorio.values().stream()
                    .filter(a -> a.getDestino().equals(destino(actor)) && !a.isLeida())
                    .forEach(Aviso::marcarLeida);
        }

        void nueva(Solicitud s) {
            save(new Aviso(s.getOrganizacionId(), s.getId(), TipoAviso.NUEVA,
                    s.getCiudadano().getNombre(), Instant.now()));
        }

        void avisar(Solicitud s, TipoAviso tipo) {
            String destino = tipo.esParaCiudadano()
                    ? s.getCiudadano().getId() : s.getOrganizacionId();
            String detalle = tipo.esParaCiudadano()
                    ? organizaciones.buscar(s.getOrganizacionId())
                            .map(OrganizacionInfo::nombre).orElse(s.getOrganizacionId())
                    : s.getCiudadano().getNombre();
            save(new Aviso(destino, s.getId(), tipo, detalle, Instant.now()));
        }

        void save(Aviso a) {
            if (a.getId() == null) {
                a.fijarId(++secuencia);
            }
            repositorio.put(a.getId(), a);
        }

        private Map<Long, Estado> estadosDe(List<Aviso> lista) {
            var ids = lista.stream().map(Aviso::getSolicitudId)
                    .filter(Objects::nonNull).collect(toSet());
            return solicitudes.todasEntidades().stream()
                    .filter(s -> ids.contains(s.getId()))
                    .collect(toMap(Solicitud::getId, Solicitud::getEstado));
        }

        private String destino(Actor actor) {
            return actor.esOrganizacion() ? organizaciones.actual(actor).id()
                    : actor.ciudadanoId();
        }

        private static AvisoInfo aInfo(Aviso a, Map<Long, Estado> estados) {
            var estado = a.getSolicitudId() != null
                    ? estados.getOrDefault(a.getSolicitudId(), a.getTipo().getEstado())
                    : a.getTipo().getEstado();
            return new AvisoInfo(a.getId(), a.getTipo(), a.getDetalle(),
                    a.getSolicitudId(), a.isLeida(), a.getCreadaEn(), estado);
        }
    }

    // Page de Spring reducida a lo que muestra la vista
    record Pagina<T>(List<T> contenido, int numero, int totalPaginas,
            long totalElementos) {
        boolean anterior() { return numero > 1; }
        boolean siguiente() { return numero < totalPaginas; }
    }

    // solicitud/ListadoService.java
    static class ListadoService {
        private static final int TAMANO_PAGINA = 12;
        private static final int TOPE_GRUPO = 20;
        private static final int TOPE_CERRADAS = 5;

        private final SolicitudService servicio;
        private final OrganizacionService organizaciones;

        ListadoService(SolicitudService servicio, OrganizacionService organizaciones) {
            this.servicio = servicio;
            this.organizaciones = organizaciones;
        }

        Pagina<SolicitudInfo> misSolicitudes(String ciudadanoId, Filtro filtro, int pagina) {
            return pagina(pagina, () -> servicio.buscar(
                    s -> s.getCiudadano().getId().equals(ciudadanoId)
                            && filtro.estados().contains(s.getEstado()),
                    Comparator.comparing(Solicitud::getCreadaEn).reversed(), null));
        }

        BandejaSolicitudes recibidas(Actor actor) {
            var org = organizacionId(actor);
            return new BandejaSolicitudes(List.of(
                    grupo(org, "org.grupo.pendientes", Set.of(Estado.PENDIENTE), TOPE_GRUPO),
                    grupo(org, "org.grupo.en-curso", Set.of(Estado.EN_CURSO), TOPE_GRUPO),
                    grupo(org, "org.grupo.cerradas", Estado.finales(), TOPE_CERRADAS)));
        }

        Pagina<SolicitudInfo> archivadas(Actor actor, int pagina) {
            Predicate<Solicitud> filtro = actor.esOrganizacion()
                    ? s -> s.getOrganizacionId().equals(organizacionId(actor))
                        && Estado.finales().contains(s.getEstado())
                    : s -> s.getCiudadano().getId().equals(actor.ciudadanoId())
                        && Estado.finales().contains(s.getEstado());
            return pagina(pagina, () -> servicio.buscar(filtro,
                    Comparator.comparing(Solicitud::getCreadaEn).reversed(), null));
        }

        long totalArchivadas(Actor actor) {
            return actor.esOrganizacion()
                    ? servicio.contar(s -> s.getOrganizacionId().equals(organizacionId(actor))
                            && Estado.finales().contains(s.getEstado()))
                    : servicio.contar(s -> s.getCiudadano().getId().equals(actor.ciudadanoId())
                            && Estado.finales().contains(s.getEstado()));
        }

        SolicitudMetricas semana(Actor actor) {
            var ahora = Instant.now();
            var desde = SolicitudMetricas.inicioDeSemana(ahora);
            Predicate<Solicitud> delActor = actor.esOrganizacion()
                    ? s -> s.getOrganizacionId().equals(organizacionId(actor))
                    : s -> s.getCiudadano().getId().equals(actor.ciudadanoId());
            var deLaSemana = servicio.buscar(
                    delActor.and(s -> !s.getCreadaEn().isBefore(desde)),
                    Comparator.comparing(Solicitud::getCreadaEn).reversed(), null);
            return SolicitudMetricas.desde(deLaSemana, ahora);
        }

        InformeMensual informe(Actor actor, YearMonth mes) {
            var desde = mes.atDay(1).atStartOfDay(Fechas.ZONA).toInstant();
            var hasta = mes.plusMonths(1).atDay(1).atStartOfDay(Fechas.ZONA).toInstant();
            Predicate<Solicitud> delActor = actor.esOrganizacion()
                    ? s -> s.getOrganizacionId().equals(organizacionId(actor))
                    : s -> s.getCiudadano().getId().equals(actor.ciudadanoId());
            var delMes = servicio.buscar(delActor.and(
                    s -> !s.getCreadaEn().isBefore(desde) && s.getCreadaEn().isBefore(hasta)),
                    Comparator.comparing(Solicitud::getCreadaEn).reversed(), null);
            return InformeMensual.desde(mes, delMes, servicio.aInfos(delMes));
        }

        List<SolicitudInfo> todasDe(String organizacionId) {
            return servicio.aInfos(servicio.buscar(
                    s -> s.getOrganizacionId().equals(organizacionId),
                    Comparator.comparing(Solicitud::getCreadaEn).reversed(), null));
        }

        private String organizacionId(Actor actor) {
            return organizaciones.actual(actor).id();
        }

        private GrupoSolicitudes grupo(String organizacionId, String clave,
                Set<Estado> estados, int tope) {
            Predicate<Solicitud> filtro = s -> s.getOrganizacionId().equals(organizacionId)
                    && estados.contains(s.getEstado());
            var lista = servicio.buscar(filtro,
                    Comparator.comparing(Solicitud::getCreadaEn).reversed(), tope);
            return new GrupoSolicitudes(clave, servicio.aInfos(lista),
                    servicio.contar(filtro));
        }

        // página visible en base 1; fuera de rango cae en la última
        private Pagina<SolicitudInfo> pagina(int numero, Supplier<List<Solicitud>> consulta) {
            var pedidas = consulta.get();
            long total = pedidas.size();
            int totalPaginas = (int) Math.max(1, Math.ceil(total / (double) TAMANO_PAGINA));
            int n = Math.min(Math.max(1, numero), totalPaginas);
            var desde = (n - 1) * TAMANO_PAGINA;
            var contenido = pedidas.subList(desde,
                    Math.min(desde + TAMANO_PAGINA, pedidas.size()));
            return new Pagina<>(servicio.aInfos(contenido), n, totalPaginas, total);
        }
    }

    // solicitud/SolicitudMetricas.java
    record SolicitudMetricas(long recibidas, long pendientes, long respondidas,
            long aceptadas, long completadas, long aceptadasCerradas,
            Duration medianaRespuesta, Duration esperaMasLarga) {

        static SolicitudMetricas ninguna() {
            return new SolicitudMetricas(0, 0, 0, 0, 0, 0, null, null);
        }

        static Instant inicioDeSemana(Instant ahora) {
            return LocalDate.ofInstant(ahora, Fechas.ZONA).with(DayOfWeek.MONDAY)
                    .atStartOfDay(Fechas.ZONA).toInstant();
        }

        static SolicitudMetricas desde(List<Solicitud> todas, Instant ahora) {
            long pendientes = 0, respondidas = 0, aceptadas = 0, completadas = 0,
                    aceptadasCerradas = 0;
            Instant pendienteMasVieja = null;
            var respuestas = new ArrayList<Duration>();
            for (var s : todas) {
                if (s.getEstado() == Estado.PENDIENTE) {
                    pendientes++;
                    if (pendienteMasVieja == null
                            || s.getCreadaEn().isBefore(pendienteMasVieja)) {
                        pendienteMasVieja = s.getCreadaEn();
                    }
                }
                if (s.getRespondidaEn() != null) {
                    respondidas++;
                    respuestas.add(Duration.between(s.getCreadaEn(), s.getRespondidaEn()));
                }
                if (s.getAceptadaEn() != null) {
                    aceptadas++;
                    if (s.getEstado().esFinal()) {
                        aceptadasCerradas++;
                    }
                }
                if (s.getEstado() == Estado.COMPLETADA) {
                    completadas++;
                }
            }
            return new SolicitudMetricas(todas.size(), pendientes, respondidas, aceptadas,
                    completadas, aceptadasCerradas, mediana(respuestas),
                    pendienteMasVieja == null ? null
                            : Duration.between(pendienteMasVieja, ahora));
        }

        String aceptacion() { return porcentaje(aceptadas, respondidas); }
        String resolucion() { return porcentaje(completadas, aceptadasCerradas); }
        String mediana() { return duracion(medianaRespuesta); }
        String espera() { return duracion(esperaMasLarga); }

        private static String porcentaje(long parte, long total) {
            return total == 0 ? "—" : String.format(Locale.ROOT, "%.0f%%", 100.0 * parte / total);
        }
        private static String duracion(Duration d) {
            if (d == null) {
                return "—";
            }
            long horas = d.toHours();
            return horas < 48 ? horas + " h" : d.toDays() + " d";
        }
        private static Duration mediana(List<Duration> tiempos) {
            if (tiempos.isEmpty()) {
                return null;
            }
            tiempos.sort(null);
            int n = tiempos.size();
            if (n % 2 == 1) {
                return tiempos.get(n / 2);
            }
            return tiempos.get(n / 2 - 1).plus(tiempos.get(n / 2)).dividedBy(2);
        }
    }

    // solicitud/InformeMensual.java
    record InformeMensual(YearMonth mes, SolicitudMetricas metricas,
            Map<Estado, Long> porEstado, Map<String, Long> porMaterial,
            List<SolicitudInfo> solicitudes) {
        static InformeMensual desde(YearMonth mes, List<Solicitud> entidades,
                List<SolicitudInfo> infos) {
            var porEstado = new EnumMap<Estado, Long>(Estado.class);
            var porMaterial = new LinkedHashMap<String, Long>();
            for (var s : entidades) {
                porEstado.merge(s.getEstado(), 1L, Long::sum);
                s.getMateriales().forEach(m -> porMaterial.merge(m.getNombre(), 1L, Long::sum));
            }
            return new InformeMensual(mes, SolicitudMetricas.desde(entidades, Instant.now()),
                    porEstado, porMaterial, infos);
        }
    }

    // solicitud/MetricasDelPeriodo.java
    record MetricasDelPeriodo(SolicitudMetricas total,
            Map<String, SolicitudMetricas> porCentro, SolicitudSeries series) { }

    // solicitud/SolicitudSeries.java
    record SolicitudSeries(List<Barra> porEstado, List<Barra> porMaterial,
            List<Barra> enElTiempo) {
        record Barra(String clave, String etiqueta, long valor, int proporcion) { }
        private record Fila(String clave, String etiqueta, long valor) { }
        private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("d/M");
        private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("M/yy");

        static SolicitudSeries desde(List<Solicitud> solicitudes, LocalDate desde,
                LocalDate hasta) {
            var estados = solicitudes.stream()
                    .collect(groupingBy(Solicitud::getEstado, counting()));
            var materiales = solicitudes.stream().flatMap(s -> s.getMateriales().stream())
                    .collect(groupingBy(Material::getNombre, counting()));
            return new SolicitudSeries(
                    barras(Arrays.stream(Estado.values())
                            .map(e -> new Fila(e.getClave(), null,
                                    estados.getOrDefault(e, 0L))).toList()),
                    barras(materiales.entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue(
                                    Comparator.reverseOrder()))
                            .map(e -> new Fila(null, e.getKey(), e.getValue())).toList()),
                    barras(tiempo(solicitudes, desde, hasta)));
        }

        private static List<Fila> tiempo(List<Solicitud> solicitudes, LocalDate desde,
                LocalDate hasta) {
            Function<Solicitud, LocalDate> dia =
                    s -> s.getCreadaEn().atZone(Fechas.ZONA).toLocalDate();
            var cuentas = new LinkedHashMap<String, Long>();
            if (desde == null) {
                var primero = solicitudes.stream().map(dia)
                        .min(Comparator.naturalOrder()).orElse(hasta);
                for (var m = YearMonth.from(primero); !m.isAfter(YearMonth.from(hasta));
                        m = m.plusMonths(1)) {
                    cuentas.put(MES.format(m), 0L);
                }
                solicitudes.forEach(s -> cuentas.merge(
                        MES.format(YearMonth.from(dia.apply(s))), 1L, Long::sum));
            } else {
                for (var d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
                    cuentas.put(DIA.format(d), 0L);
                }
                solicitudes.forEach(s -> cuentas.computeIfPresent(
                        DIA.format(dia.apply(s)), (k, v) -> v + 1));
            }
            return cuentas.entrySet().stream()
                    .map(e -> new Fila(null, e.getKey(), e.getValue())).toList();
        }

        private static List<Barra> barras(List<Fila> filas) {
            long max = filas.stream().mapToLong(Fila::valor).max().orElse(0);
            return filas.stream().map(f -> new Barra(f.clave(), f.etiqueta(), f.valor(),
                    max == 0 ? 0 : (int) Math.round(100.0 * f.valor() / max))).toList();
        }
    }

    // solicitud/MetricasService.java
    static class MetricasService {
        private final SolicitudService solicitudes;
        MetricasService(SolicitudService solicitudes) {
            this.solicitudes = solicitudes;
        }
        MetricasDelPeriodo delPeriodo(Instant desde) {
            var ahora = Instant.now();
            var creadas = desde == null ? solicitudes.todasEntidades()
                    : solicitudes.buscar(s -> !s.getCreadaEn().isBefore(desde),
                            Comparator.comparing(Solicitud::getCreadaEn), null);
            var porCentro = creadas.stream().collect(groupingBy(
                    Solicitud::getOrganizacionId,
                    Collectors.collectingAndThen(toList(),
                            l -> SolicitudMetricas.desde(l, ahora))));
            var series = SolicitudSeries.desde(creadas,
                    desde == null ? null : desde.atZone(Fechas.ZONA).toLocalDate(),
                    LocalDate.now(Fechas.ZONA));
            return new MetricasDelPeriodo(SolicitudMetricas.desde(creadas, ahora),
                    porCentro, series);
        }
    }

    // estadisticas/Periodo.java
    enum Periodo {
        MES("30d", Duration.ofDays(30)),
        TODO("todo", null);
        private final String parametro;
        private final Duration ventana;
        Periodo(String parametro, Duration ventana) {
            this.parametro = parametro;
            this.ventana = ventana;
        }
        Instant desde(Instant ahora) {
            return ventana == null ? null : ahora.minus(ventana);
        }
        static Periodo desdeParametro(String valor) {
            return Arrays.stream(values()).filter(p -> p.parametro.equals(valor))
                    .findFirst().orElse(MES);
        }
    }

    // estadisticas/EstadisticasInfo.java
    record EstadisticasInfo(Periodo periodo, String actualizado, SolicitudMetricas total,
            SolicitudSeries series, List<Centro> centros) {
        record Centro(String nombre, Ciudad ciudad, SolicitudMetricas metricas) { }
    }

    // estadisticas/EstadisticasService.java (caché de 1 día por período)
    static class EstadisticasService {
        private record Entrada(Instant calculadoEn, EstadisticasInfo info) { }
        private final Map<Periodo, Entrada> diarias = new EnumMap<>(Periodo.class);
        private final MetricasService metricas;
        private final OrganizacionService organizaciones;

        EstadisticasService(MetricasService metricas, OrganizacionService organizaciones) {
            this.metricas = metricas;
            this.organizaciones = organizaciones;
        }

        EstadisticasInfo resumen(Periodo periodo) {
            var guardada = diarias.get(periodo);
            if (guardada != null && guardada.calculadoEn()
                    .isAfter(Instant.now().minus(Duration.ofDays(1)))) {
                return guardada.info();
            }
            var info = calcular(periodo);
            diarias.put(periodo, new Entrada(Instant.now(), info));
            return info;
        }

        void olvidar() { diarias.clear(); }

        private EstadisticasInfo calcular(Periodo periodo) {
            var desde = periodo.desde(Instant.now());
            var delPeriodo = metricas.delPeriodo(desde);
            List<EstadisticasInfo.Centro> centros = organizaciones.todas().stream()
                    .map(o -> new EstadisticasInfo.Centro(o.nombre(), o.ciudad(),
                            delPeriodo.porCentro().getOrDefault(o.id(),
                                    SolicitudMetricas.ninguna())))
                    .toList();
            return new EstadisticasInfo(periodo, Fechas.corta(Instant.now()),
                    delPeriodo.total(), delPeriodo.series(), centros);
        }
    }

    // solicitud/interno/CopiaTexto.java: la copia de respaldo .txt de la org
    static final class CopiaTexto {
        private static final String CABECERA = String.join("\t", "id", "creada",
                "finalizada", "estado", "ciudad", "nombre", "contacto", "materiales",
                "direccion", "referencia", "nota");

        static String de(OrganizacionInfo org, List<SolicitudInfo> solicitudes) {
            return solicitudes.stream()
                    .map(s -> String.join("\t", String.valueOf(s.id()), fecha(s.creadaEn()),
                            fecha(s.finalizadaEn()), s.estado().name(), org.ciudad().name(),
                            limpio(s.nombreCiudadano()), limpio(s.contacto()),
                            s.materiales().stream().map(Material::getNombre)
                                    .collect(Collectors.joining(", ")),
                            limpio(s.direccion()), limpio(s.referencia()), limpio(s.nota())))
                    .collect(Collectors.joining("\n", CABECERA + "\n", "\n"));
        }

        private static String fecha(Instant instante) {
            return instante == null ? "" : instante.atZone(Fechas.ZONA).toLocalDateTime()
                    .truncatedTo(ChronoUnit.MINUTES).toString();
        }
        private static String limpio(String texto) {
            return texto == null ? "" : texto.replaceAll("[\\t\\r\\n]+", " ").strip();
        }
    }

    // ======================================================================
    // comunidad/
    // ======================================================================

    // comunidad/TipoPublicacion.java y FiltroComunidad.java
    enum TipoPublicacion { NOVEDAD, HISTORIA }
    enum FiltroComunidad {
        TODAS, NOVEDAD, HISTORIA;
        static FiltroComunidad desde(String valor) {
            try {
                return valueOf(valor.toUpperCase());
            } catch (RuntimeException e) {
                return TODAS;
            }
        }
        boolean muestra(TipoPublicacion tipo) {
            return this == TODAS || name().equals(tipo.name());
        }
    }

    // comunidad/PublicacionSemilla.java, PublicacionInfo.java, RecicladorInfo.java
    record PublicacionSemilla(String titulo, String resumen, String cuerpo,
            String imagen, String organizacion, TipoPublicacion tipo,
            Instant publicadaEn) { }
    record PublicacionInfo(long id, String titulo, String resumen, String cuerpoHtml,
            String imagen, String organizacion, TipoPublicacion tipo,
            Instant publicadaEn) {
        String fecha() { return Fechas.corta(publicadaEn); }
    }
    record RecicladorInfo(String nombre, Ciudad ciudad, String telefono) { }

    // comunidad/interno/Publicacion.java
    static class Publicacion {
        private Long id;
        private final String titulo;
        private final String resumen;
        private final String cuerpo;
        private final String imagen;
        private final String organizacion;
        private final TipoPublicacion tipo;
        private final Instant publicadaEn;

        static Publicacion nueva(PublicacionSemilla s, long id) {
            var p = new Publicacion(s.titulo(), s.resumen(), s.cuerpo(), s.imagen(),
                    s.organizacion(), s.tipo(), s.publicadaEn());
            p.id = id;
            return p;
        }
        private Publicacion(String titulo, String resumen, String cuerpo, String imagen,
                String organizacion, TipoPublicacion tipo, Instant publicadaEn) {
            this.titulo = titulo;
            this.resumen = resumen;
            this.cuerpo = cuerpo;
            this.imagen = imagen;
            this.organizacion = organizacion;
            this.tipo = tipo;
            this.publicadaEn = publicadaEn;
        }
        Long getId() { return id; }
        String getTitulo() { return titulo; }
        String getResumen() { return resumen; }
        String getCuerpo() { return cuerpo; }
        String getImagen() { return imagen; }
        String getOrganizacion() { return organizacion; }
        TipoPublicacion getTipo() { return tipo; }
        Instant getPublicadaEn() { return publicadaEn; }
    }

    // comunidad/ComunidadService.java
    static class ComunidadService {
        private static final int ULTIMAS = 10;
        private final List<Publicacion> repositorio = new ArrayList<>();
        private final List<RecicladorInfo> recicladores = new ArrayList<>();
        private long secuencia = 0;

        List<PublicacionInfo> publicadas(FiltroComunidad filtro) {
            return repositorio.stream()
                    .sorted(Comparator.comparing(Publicacion::getPublicadaEn).reversed())
                    .filter(p -> filtro.muestra(p.getTipo()))
                    .limit(ULTIMAS).map(this::aInfo).toList();
        }
        Optional<PublicacionInfo> buscar(long id) {
            return repositorio.stream().filter(p -> p.getId() == id)
                    .findFirst().map(this::aInfo);
        }
        List<RecicladorInfo> recicladores() {
            return recicladores.stream()
                    .sorted(Comparator.comparing(RecicladorInfo::nombre)).toList();
        }
        void semilla(PublicacionSemilla s) {
            repositorio.add(Publicacion.nueva(s, ++secuencia));
        }
        void semillaReciclador(RecicladorInfo r) { recicladores.add(r); }

        private PublicacionInfo aInfo(Publicacion p) {
            // Markdown de la app: en la simulación el cuerpo se muestra tal cual
            return new PublicacionInfo(p.getId(), p.getTitulo(), p.getResumen(),
                    p.getCuerpo(), p.getImagen(), p.getOrganizacion(), p.getTipo(),
                    p.getPublicadaEn());
        }
    }

    // ======================================================================
    // opinion/
    // ======================================================================

    // opinion/interno/Cuestionario.java: parseo y validación idénticos
    record Cuestionario(String titulo, List<Pregunta> preguntas) {
        static final int MAX_TEXTO = Limites.RESPUESTA_TEXTO;
        enum Tipo { ESCALA, TEXTO, OPCION }
        record Pregunta(Tipo tipo, String texto, List<String> opciones) { }
        record Respuesta(String pregunta, String valor) { }

        static Cuestionario leer(List<String> lineas) {
            String titulo = null;
            var preguntas = new ArrayList<Pregunta>();
            for (String linea : lineas) {
                linea = linea.strip();
                if (linea.isEmpty() || linea.startsWith("#")) {
                    continue;
                }
                int dosPuntos = linea.indexOf(':');
                if (dosPuntos < 0) {
                    throw new IllegalArgumentException("Línea sin tipo: " + linea);
                }
                String clave = linea.substring(0, dosPuntos).strip();
                String resto = linea.substring(dosPuntos + 1).strip();
                switch (clave) {
                    case "titulo" -> titulo = resto;
                    case "escala" -> preguntas.add(new Pregunta(Tipo.ESCALA, resto, List.of()));
                    case "texto" -> preguntas.add(new Pregunta(Tipo.TEXTO, resto, List.of()));
                    case "opcion" -> {
                        var partes = Arrays.stream(resto.split("\\|"))
                                .map(String::strip).toList();
                        if (partes.size() < 3) {
                            throw new IllegalArgumentException(
                                    "Opción con menos de dos alternativas: " + linea);
                        }
                        preguntas.add(new Pregunta(Tipo.OPCION, partes.get(0),
                                partes.subList(1, partes.size())));
                    }
                    default -> throw new IllegalArgumentException("Tipo desconocido: " + clave);
                }
            }
            if (titulo == null || preguntas.isEmpty()) {
                throw new IllegalArgumentException("Cuestionario sin título o sin preguntas");
            }
            return new Cuestionario(titulo, List.copyOf(preguntas));
        }

        Optional<List<Respuesta>> responder(IntFunction<String> valorDe) {
            var respuestas = new ArrayList<Respuesta>();
            boolean algunaEscala = false;
            for (int i = 0; i < preguntas.size(); i++) {
                var p = preguntas.get(i);
                String valor = valorDe.apply(i);
                valor = valor == null ? "" : valor.strip();
                if (valor.isEmpty()) {
                    respuestas.add(new Respuesta(p.texto(), "—"));
                    continue;
                }
                boolean valido = switch (p.tipo()) {
                    case ESCALA -> valor.matches("[1-5]");
                    case OPCION -> p.opciones().contains(valor);
                    case TEXTO -> valor.length() <= MAX_TEXTO;
                };
                if (!valido) {
                    return Optional.empty();
                }
                algunaEscala |= p.tipo() == Tipo.ESCALA;
                respuestas.add(new Respuesta(p.texto(), valor));
            }
            return algunaEscala ? Optional.of(List.copyOf(respuestas)) : Optional.empty();
        }
    }

    // ======================================================================
    // Capa "web" simulada: sesiones + controladores
    // ======================================================================

    // Una pestaña del navegador: la cookie JSESSIONID + el actor de sesión.
    // autenticado == hay Authentication en el SecurityContext de la sesión.
    static class Sesion {
        final ActorSesion actor = new ActorSesion();
        boolean autenticado;
        String ip;
        Sesion(String ip) { this.ip = ip; }
        String usuario() { return actor.actual().nombreCiudadano(); }
    }

    // HttpServletResponse reducido: código + destino del redirect + mensajes
    record RespuestaHttp(int estado, String redirect, String cuerpo,
            Map<String, String> errores) {
        static RespuestaHttp ok(String cuerpo) {
            return new RespuestaHttp(200, null, cuerpo, Map.of());
        }
        static RespuestaHttp redirect(String destino) {
            return new RespuestaHttp(302, destino, null, Map.of());
        }
        static RespuestaHttp estado(int codigo, String cuerpo) {
            return new RespuestaHttp(codigo, null, cuerpo, Map.of());
        }
        static RespuestaHttp formulario(String cuerpo, Map<String, String> errores) {
            return new RespuestaHttp(200, null, cuerpo, errores);
        }
        @Override public String toString() {
            if (redirect != null) {
                return "302 → " + redirect + (cuerpo != null ? "  («" + cuerpo + "»)" : "");
            }
            return estado + (cuerpo != null ? " " + cuerpo : "")
                    + (errores.isEmpty() ? "" : " errores=" + errores);
        }
    }

    // La aplicación completa: repositorios + servicios + endpoints
    static class App {
        final UsuariosRepo usuarios = new UsuariosRepo();
        final Cuentas cuentas = new Cuentas(usuarios);
        final Intentos intentos = new Intentos(5);
        final CatalogoMateriales catalogo = new CatalogoMateriales();
        final OrganizacionService organizaciones = new OrganizacionService(catalogo);
        final AvisoService avisos = new AvisoService();
        final SolicitudService solicitudes = new SolicitudService(avisos, organizaciones, catalogo);
        final ListadoService listados = new ListadoService(solicitudes, organizaciones);
        final MetricasService metricas = new MetricasService(solicitudes);
        final EstadisticasService estadisticas = new EstadisticasService(metricas, organizaciones);
        final ComunidadService comunidad = new ComunidadService();
        final Map<Rol, Cuestionario> cuestionarios = new EnumMap<>(Rol.class);
        final List<Cuestionario.Respuesta> respuestasOpinion = new ArrayList<>();

        App() {
            avisos.vincular(solicitudes, organizaciones);
        }

        // ---------- SeguridadConfig: reglas de acceso por ruta ----------
        private boolean privada(String ruta) {
            return ruta.startsWith(Rutas.NUEVA) || ruta.startsWith(Rutas.MIS_SOLICITUDES)
                    || ruta.startsWith(Rutas.AVISOS) || ruta.startsWith(Rutas.OPINION);
        }
        private boolean deOrganizacion(String ruta) {
            return ruta.startsWith("/org/") || ruta.startsWith("/demo/");
        }
        // null si el acceso está permitido; si no, la respuesta de la cadena
        private RespuestaHttp controlar(Sesion s, String ruta) {
            if (privada(ruta) && !s.autenticado) {
                return RespuestaHttp.redirect(Rutas.CUENTA_ENTRAR);
            }
            if (deOrganizacion(ruta)
                    && !(s.autenticado && s.actor.actual().esOrganizacion())) {
                return RespuestaHttp.estado(403, "Prohibido");
            }
            return null;
        }

        // ---------- CuentaController + formLogin + IngresoHandler ----------

        // POST /cuenta/registrar
        RespuestaHttp postRegistrar(Sesion s, String nombre, String pais,
                String telefono, String clave, String condiciones) {
            Map<String, String> errores = new LinkedHashMap<>();
            if (condiciones == null) {
                errores.put("condiciones", "Tenés que aceptar las condiciones de uso");
            }
            Pais p;
            try {
                p = Pais.valueOf(pais == null ? "" : pais);
            } catch (IllegalArgumentException e) {
                p = null;
                errores.put("pais", "Elegí Uruguay o Brasil");
            }
            if (p != null) {
                try {
                    cuentas.validarDatos(nombre, telefono, p, clave);
                } catch (CuentaException e) {
                    errores.put(e.campo(), e.getMessage());
                }
            }
            if (!errores.isEmpty()) {
                return RespuestaHttp.formulario("secciones/cuenta/registrar", errores);
            }
            var usuario = cuentas.registrarCiudadano(nombre.strip(), telefono, p, clave);
            var principal = new UsuarioPrincipal(
                    usuarios.findByTelefono(usuario.telefono()).orElseThrow());
            // registro = entrada inmediata (misma semántica que IngresoHandler)
            s.autenticado = true;
            s.actor.autenticar(principal.getCiudadanoId(), principal.getNombre(),
                    principal.getRol(), principal.getOrganizacionId());
            return new RespuestaHttp(302, Seccion.inicioDe(principal.getRol()),
                    "¡Cuenta creada, " + principal.getNombre()
                            + "! Ya podés pedir tu primer retiro.", Map.of());
        }

        // POST /cuenta/entrar (formLogin: username = nombre de usuario)
        RespuestaHttp postEntrar(Sesion s, String username, String password) {
            if (!intentos.permitir(s.ip)) {
                return RespuestaHttp.estado(429,
                        "Demasiados intentos. Espera un minuto e intenta de nuevo.");
            }
            var encontrado = usuarios.findByNombre(
                    username == null ? "" : username.strip().toLowerCase());
            if (encontrado.isEmpty()
                    || !Cifrado.matches(password, encontrado.get().getClaveHash())) {
                return RespuestaHttp.redirect(Rutas.CUENTA_ENTRAR + "?error");
            }
            var principal = new UsuarioPrincipal(encontrado.get());
            s.autenticado = true;
            s.actor.autenticar(principal.getCiudadanoId(), principal.getNombre(),
                    principal.getRol(), principal.getOrganizacionId());
            return RespuestaHttp.redirect(Seccion.inicioDe(principal.getRol()));
        }

        // POST /cuenta/salir
        RespuestaHttp postSalir(Sesion s) {
            s.autenticado = false;
            s.actor.autenticar("ciudadano-demo", "Martina López", Rol.CIUDADANO, null);
            return RespuestaHttp.redirect(Rutas.RAIZ);
        }

        // ---------- NuevaController ----------

        private static final List<String> CAMPOS_TEXTO = List.of(
                "nombre", "contacto", "direccion", "referencia", "nota");

        // GET /nueva: devuelve los centros de la ciudad y los materiales del
        // centro elegido; el nombre viene precargado con el del actor.
        RespuestaHttp getNueva(Sesion s, Ciudad ciudad, String organizacionId,
                List<Material> materialesMarcados) {
            var denegado = controlar(s, Rutas.NUEVA);
            if (denegado != null) {
                return denegado;
            }
            var centros = ciudad == null ? List.<OrganizacionInfo>of()
                    : organizaciones.enCiudad(ciudad);
            var centro = centros.stream()
                    .filter(o -> o.id().equals(organizacionId)).findFirst();
            var marcados = new ArrayList<>(materialesMarcados == null
                    ? List.<Material>of() : materialesMarcados);
            centro.ifPresentOrElse(
                    o -> marcados.retainAll(o.materiales().stream()
                            .filter(Material::isActivo).toList()),
                    marcados::clear);
            var sb = new StringBuilder("GET /nueva");
            sb.append("  nombre precargado: ").append(s.usuario()).append('\n');
            sb.append("  centros en ").append(ciudad == null ? "—" : ciudad.nombre())
                    .append(": ");
            centros.forEach(o -> sb.append('[').append(o.id()).append("] ")
                    .append(o.nombre()).append("  "));
            if (centros.isEmpty()) {
                sb.append("(ninguno)");
            }
            sb.append("\n  materiales marcados que siguen válidos: ")
                    .append(marcados.stream().map(Material::getCodigo).toList());
            return RespuestaHttp.ok(sb.toString());
        }

        // POST /nueva/validar: validación al salir de un campo (blur)
        RespuestaHttp postNuevaValidar(Sesion s, String campo, String valor) {
            var denegado = controlar(s, Rutas.NUEVA_VALIDAR);
            if (denegado != null) {
                return denegado;
            }
            if (!CAMPOS_TEXTO.contains(campo)) {
                return RespuestaHttp.ok("");
            }
            return RespuestaHttp.ok(validarCampoNueva(campo, valor));
        }

        private String validarCampoNueva(String campo, String valor) {
            int max = switch (campo) {
                case "nombre" -> Limites.NOMBRE;
                case "contacto" -> Limites.CONTACTO;
                case "direccion", "referencia" -> Limites.REFERENCIA;
                case "nota" -> Limites.NOTA;
                default -> Integer.MAX_VALUE;
            };
            boolean obligatorio = switch (campo) {
                case "nombre", "contacto", "direccion" -> true;
                default -> false;
            };
            if (obligatorio && (valor == null || valor.isBlank())) {
                return switch (campo) {
                    case "nombre" -> "Ingresá tu nombre";
                    case "contacto" -> "Ingresá un contacto";
                    default -> "Ingresá una dirección";
                };
            }
            if (valor != null && valor.length() > max) {
                return "Máximo " + max + " caracteres";
            }
            return "";
        }

        // POST /nueva
        RespuestaHttp postNueva(Sesion s, Ciudad ciudad, List<Material> materiales,
                String organizacionId, String nombre, String contacto,
                String direccion, String referencia, Double latitud,
                Double longitud, String nota) {
            var denegado = controlar(s, Rutas.NUEVA);
            if (denegado != null) {
                return denegado;
            }
            Map<String, String> errores = new LinkedHashMap<>();
            // Bean Validation de NuevaForm
            if (ciudad == null) {
                errores.put("ciudad", "Elegí una ciudad válida");
            }
            if (materiales == null || materiales.isEmpty()) {
                errores.put("materiales", "Elegí al menos un material");
            }
            if (organizacionId == null || organizacionId.isBlank()) {
                errores.put("organizacionId",
                        "La organización no recibe esos materiales en esa ciudad");
            }
            campo(errores, "nombre", nombre, true, Limites.NOMBRE, "Ingresá tu nombre");
            campo(errores, "contacto", contacto, true, Limites.CONTACTO, "Ingresá un contacto");
            campo(errores, "direccion", direccion, true, Limites.DIRECCION, "Ingresá una dirección");
            campo(errores, "referencia", referencia, false, Limites.REFERENCIA, null);
            campo(errores, "nota", nota, false, Limites.NOTA, null);

            var centros = ciudad == null ? List.<OrganizacionInfo>of()
                    : organizaciones.enCiudad(ciudad);
            var centro = centros.stream()
                    .filter(o -> o.id().equals(organizacionId)).findFirst();
            if (errores.isEmpty() && centro.isEmpty()) {
                errores.put("organizacionId", "Elegí un centro de acopio de la ciudad");
            }
            if (errores.isEmpty() && !centro.get().recibeTodos(materiales)) {
                errores.put("materiales",
                        "Ese material no lo recibe el centro elegido. Desmarcalo o elegí otro centro.");
            }
            boolean parIncompleto = latitud == null != (longitud == null);
            Ubicacion ubicacion = null;
            boolean fueraDeRango = false;
            if (latitud != null && longitud != null) {
                try {
                    ubicacion = new Ubicacion(latitud, longitud);
                } catch (IllegalArgumentException e) {
                    fueraDeRango = true;
                }
            }
            if (parIncompleto || fueraDeRango) {
                errores.put("latitud",
                        "La ubicación no quedó completa; tocá \"Usar mi ubicación\" de nuevo");
            }
            if (!errores.isEmpty()) {
                return RespuestaHttp.formulario("secciones/nueva", errores);
            }
            SolicitudCreacion datos;
            try {
                datos = new SolicitudCreacion(ciudad, direccion, referencia, ubicacion,
                        materiales, organizacionId, nombre, contacto, nota);
            } catch (IllegalArgumentException e) {
                return RespuestaHttp.estado(400, "SolicitudCreacion inválida");
            }
            var creada = solicitudes.crear(s.actor.actual(), datos);
            if (creada.isPresent()) {
                return new RespuestaHttp(302, Rutas.MIS_SOLICITUDES,
                        "Solicitud #" + creada.get().id() + " enviada a "
                                + creada.get().organizacionNombre()
                                + " · te avisamos cuando respondan", Map.of());
            }
            return RespuestaHttp.formulario("secciones/nueva", Map.of("organizacionId",
                    "La organización no recibe esos materiales en esa ciudad"));
        }

        private static void campo(Map<String, String> errores, String nombre,
                String valor, boolean obligatorio, int max, String msgVacio) {
            if (valor == null || valor.isBlank()) {
                if (obligatorio) {
                    errores.put(nombre, msgVacio);
                }
            } else if (valor.length() > max) {
                errores.put(nombre, "Máximo " + max + " caracteres");
            }
        }

        // ---------- MisSolicitudesController ----------

        RespuestaHttp getMisSolicitudes(Sesion s, String filtro, int pagina) {
            var denegado = controlar(s, Rutas.MIS_SOLICITUDES);
            if (denegado != null) {
                return denegado;
            }
            var activo = Filtro.desde(filtro);
            var p = listados.misSolicitudes(s.actor.actual().ciudadanoId(), activo, pagina);
            var sb = new StringBuilder("GET /mis-solicitudes  usuario=")
                    .append(s.usuario()).append("  filtro=").append(activo)
                    .append("  página ").append(p.numero()).append('/')
                    .append(p.totalPaginas()).append(" (total ").append(p.totalElementos())
                    .append(")\n");
            p.contenido().forEach(i -> sb.append("  #").append(i.id()).append(' ')
                    .append(i.estado().etiqueta()).append(" → ")
                    .append(i.organizacionNombre()).append("  ")
                    .append(i.materiales().stream().map(Material::getNombre).toList())
                    .append("  creada ").append(i.creada()).append('\n'));
            var semana = listados.semana(s.actor.actual());
            sb.append("  semana: ").append(semana.recibidas()).append(" recibidas, ")
                    .append(semana.pendientes()).append(" pendientes; archivadas: ")
                    .append(listados.totalArchivadas(s.actor.actual()));
            return RespuestaHttp.ok(sb.toString());
        }

        RespuestaHttp getMisSolicitud(Sesion s, long id) {
            var denegado = controlar(s, Rutas.MIS_SOLICITUDES + "/" + id);
            if (denegado != null) {
                return denegado;
            }
            return solicitudes.detalle(s.actor.actual(), id)
                    .<RespuestaHttp>map(i -> RespuestaHttp.ok("GET /mis-solicitudes/" + id
                            + "  " + i.estado().etiqueta() + " → " + i.organizacionNombre()
                            + (i.tieneDetalle() ? "  (tiene detalle)" : "")))
                    .orElse(RespuestaHttp.estado(404, "No encontrada"));
        }

        // POST /mis-solicitudes/{id}/cancelar
        RespuestaHttp postCancelar(Sesion s, long id, long version) {
            var denegado = controlar(s, Rutas.MIS_SOLICITUDES + "/" + id + "/cancelar");
            if (denegado != null) {
                return denegado;
            }
            return responder(solicitudes.cancelar(s.actor.actual(), id, version),
                    "Solicitud #" + id + " cancelada · quedó sin efecto",
                    id, Rutas.MIS_SOLICITUDES);
        }

        // ---------- OrgSolicitudesController ----------

        RespuestaHttp getOrgSolicitudes(Sesion s) {
            var denegado = controlar(s, Rutas.ORG_SOLICITUDES);
            if (denegado != null) {
                return denegado;
            }
            var org = organizaciones.actual(s.actor.actual());
            var bandeja = listados.recibidas(s.actor.actual());
            var sb = new StringBuilder("GET /org/solicitudes  centro=")
                    .append(org.nombre()).append('\n');
            for (var g : bandeja.grupos()) {
                sb.append("  ").append(Mensajes.t(g.clave())).append(" (")
                        .append(g.solicitudes().size()).append(" de ").append(g.total())
                        .append("):\n");
                g.solicitudes().forEach(i -> sb.append("    #").append(i.id())
                        .append(' ').append(i.nombreCiudadano()).append("  ")
                        .append(i.materiales().stream().map(Material::getNombre).toList())
                        .append("  ").append(i.creada()).append('\n'));
            }
            var semana = listados.semana(s.actor.actual());
            sb.append("  semana: ").append(semana.recibidas()).append(" recibidas, ")
                    .append(semana.respondidas()).append(" respondidas, aceptación ")
                    .append(semana.aceptacion()).append(", resolución ")
                    .append(semana.resolucion()).append(", espera ").append(semana.espera())
                    .append("\n  archivadas: ")
                    .append(listados.totalArchivadas(s.actor.actual()));
            return RespuestaHttp.ok(sb.toString());
        }

        RespuestaHttp getOrgArchivadas(Sesion s, int pagina) {
            var denegado = controlar(s, Rutas.ORG_ARCHIVADAS);
            if (denegado != null) {
                return denegado;
            }
            var p = listados.archivadas(s.actor.actual(), pagina);
            var sb = new StringBuilder("GET /org/solicitudes/archivadas  página ")
                    .append(p.numero()).append('/').append(p.totalPaginas())
                    .append(" (total ").append(p.totalElementos()).append(")\n");
            p.contenido().forEach(i -> sb.append("  #").append(i.id()).append(' ')
                    .append(i.estado().etiqueta()).append(' ')
                    .append(i.nombreCiudadano()).append("  cerrada ")
                    .append(i.finalizada()).append('\n'));
            return RespuestaHttp.ok(sb.toString());
        }

        RespuestaHttp getOrgInforme(Sesion s, String mes) {
            var denegado = controlar(s, Rutas.ORG_INFORME);
            if (denegado != null) {
                return denegado;
            }
            return RespuestaHttp.ok(informeTexto(s, mes));
        }

        RespuestaHttp getMisInforme(Sesion s, String mes) {
            var denegado = controlar(s, Rutas.MIS_INFORME);
            if (denegado != null) {
                return denegado;
            }
            return RespuestaHttp.ok(informeTexto(s, mes));
        }

        private String informeTexto(Sesion s, String mes) {
            YearMonth m;
            try {
                m = mes == null ? YearMonth.now() : YearMonth.parse(mes);
            } catch (Exception e) {
                m = YearMonth.now();
            }
            var informe = listados.informe(s.actor.actual(), m);
            var sb = new StringBuilder("informe ").append(m).append(": ");
            informe.porEstado().forEach((e, n) ->
                    sb.append(e.etiqueta()).append('=').append(n).append(' '));
            sb.append("\n  materiales: ").append(informe.porMaterial());
            return sb.toString();
        }

        // GET /org/solicitudes/copia: solo con org válida en sesión (sin
        // fallback a la primera, a diferencia de actual())
        RespuestaHttp getOrgCopia(Sesion s) {
            var denegado = controlar(s, Rutas.ORG_COPIA);
            if (denegado != null) {
                return denegado;
            }
            var actor = s.actor.actual();
            var org = Optional.of(actor).filter(Actor::esOrganizacion)
                    .map(Actor::organizacionId).flatMap(organizaciones::buscar);
            if (org.isEmpty()) {
                return RespuestaHttp.estado(403, "Prohibido");
            }
            return RespuestaHttp.ok(CopiaTexto.de(org.get(), listados.todasDe(org.get().id())));
        }

        RespuestaHttp getOrgSolicitud(Sesion s, long id) {
            var denegado = controlar(s, Rutas.ORG_SOLICITUDES + "/" + id);
            if (denegado != null) {
                return denegado;
            }
            return solicitudes.detalle(s.actor.actual(), id)
                    .<RespuestaHttp>map(i -> RespuestaHttp.ok("GET /org/solicitudes/" + id
                            + "  " + i.estado().etiqueta() + " de " + i.nombreCiudadano()
                            + "  contacto " + i.contacto()))
                    .orElse(RespuestaHttp.estado(404, "No encontrada"));
        }

        RespuestaHttp postOrgAceptar(Sesion s, long id, long version) {
            return accionOrg(s, id, version, "aceptar");
        }
        RespuestaHttp postOrgRechazar(Sesion s, long id, long version) {
            return accionOrg(s, id, version, "rechazar");
        }
        RespuestaHttp postOrgCompletar(Sesion s, long id, long version) {
            return accionOrg(s, id, version, "completar");
        }

        private RespuestaHttp accionOrg(Sesion s, long id, long version, String accion) {
            var denegado = controlar(s, Rutas.ORG_SOLICITUDES + "/" + id + "/" + accion);
            if (denegado != null) {
                return denegado;
            }
            var actor = s.actor.actual();
            var resultado = switch (accion) {
                case "aceptar" -> solicitudes.aceptar(actor, id, version);
                case "rechazar" -> solicitudes.rechazar(actor, id, version);
                default -> solicitudes.completar(actor, id, version);
            };
            var ok = switch (accion) {
                case "aceptar" -> "Solicitud #" + id + " aceptada · ya avisamos al vecino";
                case "rechazar" -> "Solicitud #" + id + " rechazada · el vecino ya lo sabe";
                default -> "Solicitud #" + id + " completada · el material ya llegó al centro";
            };
            return responder(resultado, ok, id, Rutas.ORG_SOLICITUDES);
        }

        // Respuestas.responder: el resultado como redirect con flash
        private RespuestaHttp responder(Resultado resultado, String ok, long id,
                String destino) {
            return switch (resultado) {
                case OK -> new RespuestaHttp(302, destino, ok, Map.of());
                case INVALIDA -> new RespuestaHttp(302, destino,
                        "La solicitud #" + id + " no admite esa acción ahora", Map.of());
                case CONFLICTO -> new RespuestaHttp(302, destino,
                        "La solicitud #" + id + " cambió antes de tu acción; recargá la página",
                        Map.of());
                case NO_ENCONTRADA -> RespuestaHttp.estado(404, "No encontrada");
                case SIN_PERMISO -> RespuestaHttp.estado(403, "Prohibido");
            };
        }

        // ---------- Accesos para la capa de consola (Ui) ----------

        // NuevaController.nueva(): centros de la ciudad y marcados que siguen
        // valiendo para el centro elegido (retainAll sobre activos)
        List<OrganizacionInfo> centrosDe(Ciudad ciudad) {
            return ciudad == null ? List.of() : organizaciones.enCiudad(ciudad);
        }

        // Resultado → flash, mismos textos que messages.properties
        String textoResultado(Resultado resultado, String ok, long id) {
            return switch (resultado) {
                case OK -> ok;
                case INVALIDA -> "La solicitud #" + id + " no admite esa acción ahora";
                case CONFLICTO -> "La solicitud #" + id
                        + " cambió antes de tu acción; recargá la página";
                case NO_ENCONTRADA -> "404 · No encontrada";
                case SIN_PERMISO -> "403 · Prohibido";
            };
        }

        // ---------- AvisosController ----------

        RespuestaHttp getAvisos(Sesion s) {
            var denegado = controlar(s, Rutas.AVISOS);
            if (denegado != null) {
                return denegado;
            }
            var actor = s.actor.actual();
            var lista = avisos.avisosPara(actor);
            var sb = new StringBuilder("GET /avisos  destino=").append(
                    actor.esOrganizacion() ? organizaciones.actual(actor).id()
                            : actor.ciudadanoId()).append('\n');
            lista.forEach(a -> sb.append("  ").append(a.leida() ? "[leída] " : "[nueva] ")
                    .append(textoAviso(a)).append("  (estado actual: ")
                    .append(a.estado().etiqueta()).append(")\n"));
            if (actor.esOrganizacion()) {
                avisos.kanbanPara(actor).forEach(c -> sb.append("  columna ")
                        .append(Mensajes.t(c.clave())).append(": ")
                        .append(c.avisos().size()).append('\n'));
            }
            avisos.marcarLeidas(actor);
            return RespuestaHttp.ok(sb.toString());
        }

        RespuestaHttp getAvisosContador(Sesion s) {
            var denegado = controlar(s, Rutas.AVISOS_CONTADOR);
            if (denegado != null) {
                return denegado;
            }
            return RespuestaHttp.ok(
                    String.valueOf(avisos.avisosSinLeer(s.actor.actual())));
        }

        private static String textoAviso(AvisoInfo a) {
            return Mensajes.t(a.tipo().getClave(), a.detalle())
                    + " (#" + a.solicitudId() + ")";
        }

        // ---------- AcopiosController ----------

        RespuestaHttp getAcopios(Sesion s) {
            var sb = new StringBuilder("GET /acopios\n");
            for (var grupo : organizaciones.porCiudad()) {
                sb.append("  ").append(grupo.ciudad().nombre()).append(":\n");
                grupo.organizaciones().forEach(o -> sb.append("    ")
                        .append(o.nombre()).append("  ").append(o.horario())
                        .append("  ").append(o.telefono())
                        .append(o.ubicacion() != null
                                ? "  " + o.ubicacion().enlaceMapa() : "")
                        .append('\n'));
            }
            return RespuestaHttp.ok(sb.toString());
        }

        // ---------- PerfilController ----------

        RespuestaHttp postOrgPerfil(Sesion s, List<Material> materiales,
                String horario, String telefono, Double latitud, Double longitud,
                long version) {
            var denegado = controlar(s, Rutas.ORG_PERFIL);
            if (denegado != null) {
                return denegado;
            }
            Map<String, String> errores = new LinkedHashMap<>();
            if (materiales == null || materiales.isEmpty()) {
                errores.put("materiales", "Marcá al menos un material que recibe el acopio");
            }
            if (horario == null || horario.isBlank()) {
                errores.put("horario", "Ingresá un horario válido");
            } else if (horario.length() > Limites.HORARIO) {
                errores.put("horario", "Máximo " + Limites.HORARIO + " caracteres");
            }
            if (telefono == null || telefono.isBlank()) {
                errores.put("telefono", "Falta el teléfono del acopio");
            } else if (!telefono.matches(Organizacion.PATRON_TELEFONO_O_VACIO)) {
                errores.put("telefono", "El teléfono no es válido: incluí el código de país "
                        + "y al menos 8 dígitos (ej.: +598 92 000 111)");
            }
            boolean parIncompleto = latitud == null != (longitud == null);
            Ubicacion ubicacion = null;
            boolean fueraDeRango = false;
            if (latitud != null && longitud != null) {
                try {
                    ubicacion = new Ubicacion(latitud, longitud);
                } catch (IllegalArgumentException e) {
                    fueraDeRango = true;
                }
            }
            if (parIncompleto || fueraDeRango) {
                errores.put("latitud",
                        "La ubicación no quedó completa; tocá \"Usar mi ubicación\" de nuevo");
            }
            if (!errores.isEmpty()) {
                return RespuestaHttp.formulario("secciones/org/perfil", errores);
            }
            var org = organizaciones.actual(s.actor.actual());
            if (!organizaciones.actualizarPerfil(org.id(), materiales, horario,
                    telefono, ubicacion, version)) {
                return RespuestaHttp.formulario("secciones/org/perfil (conflicto)",
                        Map.of("conflicto", "El perfil cambió antes de tu guardado"));
            }
            return new RespuestaHttp(302, Rutas.ORG_PERFIL, "guardado", Map.of());
        }

        // ---------- ComunidadController ----------

        RespuestaHttp getComunidad(String tipo) {
            var activo = FiltroComunidad.desde(tipo);
            var sb = new StringBuilder("GET /comunidad  filtro=").append(activo).append('\n');
            comunidad.publicadas(activo).forEach(p -> sb.append("  [")
                    .append(p.tipo()).append("] ").append(p.titulo())
                    .append(" — ").append(p.organizacion()).append('\n'));
            sb.append("  recicladores del padrón:");
            comunidad.recicladores().forEach(r -> sb.append(' ').append(r.nombre())
                    .append('(').append(r.ciudad()).append(")"));
            return RespuestaHttp.ok(sb.toString());
        }

        // ---------- EstadisticasController ----------

        RespuestaHttp getEstadisticas(String periodo) {
            var p = Periodo.desdeParametro(periodo);
            var info = estadisticas.resumen(p);
            var sb = new StringBuilder("GET /estadisticas  período=")
                    .append(periodo == null ? "30d" : periodo).append("  actualizado ")
                    .append(info.actualizado()).append('\n');
            var t = info.total();
            sb.append("  total: ").append(t.recibidas()).append(" recibidas, ")
                    .append(t.respondidas()).append(" respondidas, aceptación ")
                    .append(t.aceptacion()).append(", resolución ").append(t.resolucion())
                    .append(", mediana respuesta ").append(t.mediana()).append('\n');
            info.centros().forEach(c -> sb.append("  ").append(c.nombre())
                    .append(" (").append(c.ciudad()).append("): ")
                    .append(c.metricas().recibidas()).append(" recibidas, aceptación ")
                    .append(c.metricas().aceptacion()).append('\n'));
            sb.append("  serie en el tiempo: ");
            info.series().enElTiempo().forEach(b -> sb.append(b.etiqueta())
                    .append('=').append(b.valor()).append(' '));
            return RespuestaHttp.ok(sb.toString());
        }

        // ---------- OpinionController ----------

        RespuestaHttp postOpinionRespaldo(Sesion s, Map<Integer, String> params) {
            var denegado = controlar(s, Rutas.OPINION_RESPALDO);
            if (denegado != null) {
                return denegado;
            }
            var rol = s.actor.get();
            var cuestionario = cuestionarios.get(rol);
            var respuestas = cuestionario.responder(i -> params.get(i));
            if (respuestas.isEmpty()) {
                return RespuestaHttp.redirect(
                        Rutas.OPINION + "?respaldo=incompleto#respaldo");
            }
            respuestasOpinion.addAll(respuestas.get());
            return RespuestaHttp.redirect(Rutas.OPINION + "?respaldo=gracias#respaldo");
        }
    }

    // ======================================================================
    // Datos iniciales: DemoDatos + CuentaSeeder + cuestionarios de respaldo
    // ======================================================================

    static void sembrar(App app) {
        app.catalogo.semillarFaltantes();
        var cat = app.catalogo;

        // DemoDatos.organizaciones
        app.organizaciones.guardar(new Organizacion("frontera-limpia",
                "Centro de ejemplo – Paso del Enano", Ciudad.RIVERA,
                List.of(cat.resolver("PLASTICO"), cat.resolver("CARTON"),
                        cat.resolver("PAPEL"), cat.resolver("VIDRIO"),
                        cat.resolver("METAL"), cat.resolver("LATAS")),
                "Lunes a sábado, 7–19 h", "+598 00 000 001",
                new Ubicacion(-30.9120, -55.5560)));
        app.organizaciones.guardar(new Organizacion("acopio-verde",
                "Centro de ejemplo – Microcentro", Ciudad.RIVERA,
                List.of(cat.resolver("CARTON"), cat.resolver("PAPEL"),
                        cat.resolver("PLASTICO"), cat.resolver("LATAS")),
                "Lunes a viernes, 8–17 h", "+598 00 000 002",
                new Ubicacion(-30.9050, -55.5508)));
        app.organizaciones.guardar(new Organizacion("coleta-solidaria",
                "Centro de exemplo – Jardins", Ciudad.LIVRAMENTO,
                List.of(cat.resolver("PLASTICO"), cat.resolver("CARTON"),
                        cat.resolver("PAPEL"), cat.resolver("VIDRIO"),
                        cat.resolver("METAL"), cat.resolver("LATAS")),
                "Consultar", "+55 00 0000-0003",
                new Ubicacion(-30.8900, -55.5320)));

        // CuentaSeeder: una cuenta de organización por ciudad, clave "centro-demo"
        record Centro(String usuario, String organizacionId) { }
        var centros = List.of(
                new Centro("renacer-norte", "frontera-limpia"),
                new Centro("novo-horizonte", "coleta-solidaria"));
        int n = 0;
        for (var c : centros) {
            app.usuarios.save(new UsuarioEntidad(UUID.randomUUID().toString(),
                    "59800000" + n, Cifrado.encode("centro-demo"), Rol.ORGANIZACION,
                    c.organizacionId(), c.usuario()));
            n++;
        }

        // DemoDatos.solicitudes
        var ahora = Instant.now();
        var semillas = List.of(
                new SolicitudSemilla("ciudadano-demo", "Martina López", "099 000 001",
                        "Centro de Rivera, cerca de Plaza Internacional", null, null,
                        List.of(cat.resolver("CARTON"), cat.resolver("PAPEL")),
                        "frontera-limpia", "Portón verde", Estado.COMPLETADA,
                        ahora.minus(5, ChronoUnit.DAYS),
                        ahora.minus(5, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS),
                        ahora.minus(4, ChronoUnit.DAYS)),
                new SolicitudSemilla("ciudadano-demo", "Martina López", "099 000 001",
                        "Centro de Rivera", null, null,
                        List.of(cat.resolver("PLASTICO")),
                        "frontera-limpia", null, Estado.EN_CURSO,
                        ahora.minus(3, ChronoUnit.DAYS),
                        ahora.minus(3, ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS), null),
                new SolicitudSemilla("vecina-ana", "Ana Rodríguez", "099 000 002",
                        "Rivera, zona centro", null, null,
                        List.of(cat.resolver("PLASTICO"), cat.resolver("METAL")),
                        "frontera-limpia", "Fondo", Estado.PENDIENTE,
                        ahora.minus(2, ChronoUnit.DAYS), null, null),
                new SolicitudSemilla("vecino-bruno", "Bruno Pérez", "099 000 003",
                        "Rivera", null, null,
                        List.of(cat.resolver("CARTON")),
                        "acopio-verde", "Dejar en la vereda", Estado.PENDIENTE,
                        ahora.minus(1, ChronoUnit.DAYS), null, null),
                new SolicitudSemilla("vecina-carla", "Carla Santos", "(55) 90000-0004",
                        "Bairro Parque do Sol, Livramento", null, null,
                        List.of(cat.resolver("VIDRIO")),
                        "coleta-solidaria", "Fora do horário", Estado.RECHAZADA,
                        ahora.minus(6, ChronoUnit.DAYS),
                        ahora.minus(6, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS),
                        ahora.minus(6, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS)),
                new SolicitudSemilla("vecino-joao", "João Almeida", "(55) 90000-0005",
                        "Bairro Cohab do Armour, Livramento", null, null,
                        List.of(cat.resolver("PLASTICO"), cat.resolver("LATAS")),
                        "coleta-solidaria", "Deixar no portão", Estado.PENDIENTE,
                        ahora.minus(1, ChronoUnit.DAYS), null, null));
        for (var s : semillas) {
            var ciudadano = app.solicitudes.ciudadanoSemilla(s.ciudadanoId(),
                    s.nombreCiudadano());
            app.solicitudes.save(Solicitud.semilla(s, ciudadano));
        }

        // DemoDatos.avisos (atan a ids reales por nombre + estado)
        var todas = app.solicitudes.todas();
        Function<String, Function<Estado, Long>> idDe = nombre -> estado ->
                todas.stream().filter(i -> i.nombreCiudadano().equals(nombre)
                        && i.estado() == estado).map(SolicitudInfo::id)
                        .findFirst().orElse(null);
        var avisoSemillas = List.of(
                new AvisoSemilla("frontera-limpia",
                        idDe.apply("Ana Rodríguez").apply(Estado.PENDIENTE),
                        TipoAviso.NUEVA, "Ana Rodríguez", false,
                        ahora.minus(2, ChronoUnit.DAYS)),
                new AvisoSemilla("ciudadano-demo",
                        idDe.apply("Martina López").apply(Estado.EN_CURSO),
                        TipoAviso.ACEPTADA, "Centro de ejemplo – Paso del Enano",
                        false, ahora.minus(3, ChronoUnit.DAYS)),
                new AvisoSemilla("ciudadano-demo",
                        idDe.apply("Martina López").apply(Estado.COMPLETADA),
                        TipoAviso.COMPLETADA, "Centro de ejemplo – Paso del Enano",
                        true, ahora.minus(4, ChronoUnit.DAYS)));
        for (var a : avisoSemillas) {
            var aviso = new Aviso(a.destino(), a.solicitudId(), a.tipo(),
                    a.detalle(), a.creadaEn());
            if (a.leida()) {
                aviso.marcarLeida();
            }
            app.avisos.save(aviso);
        }

        // DemoDatos.recicladores
        List.of(
                new RecicladorInfo("Luis Pereira", Ciudad.RIVERA, "+598 00 000 011"),
                new RecicladorInfo("María Dos Santos", Ciudad.LIVRAMENTO, null),
                new RecicladorInfo("Oscar Ríos", Ciudad.RIVERA, "+598 00 000 012"),
                new RecicladorInfo("José Silva", Ciudad.LIVRAMENTO, "+55 00 0000-0013"),
                new RecicladorInfo("Ana Costa", Ciudad.RIVERA, null)
        ).forEach(app.comunidad::semillaReciclador);

        // DemoDatos.comunidad
        List.of(
                new PublicacionSemilla(
                        "Renacer del Norte: ocho años y 27 familias",
                        "La cooperativa de clasificadores de Rivera cumple ocho años.",
                        "Cuerpo completo en Markdown…", null, "Renacer del Norte",
                        TipoPublicacion.HISTORIA, Instant.parse("2026-10-12T12:00:00Z")),
                new PublicacionSemilla(
                        "Contenedores en casa: plan piloto en el barrio Racca",
                        "La Intendencia entrega contenedores gratis en el barrio Racca.",
                        "Cuerpo completo en Markdown…", null, "Intendencia de Rivera",
                        TipoPublicacion.NOVEDAD, Instant.parse("2026-09-28T12:00:00Z")),
                new PublicacionSemilla(
                        "Nuevo Punto Verde en el Polo de Desarrollo",
                        "Apertura del Punto Verde 2 en el Polo de Desarrollo.",
                        "Cuerpo completo en Markdown…", null, "Intendencia de Rivera",
                        TipoPublicacion.NOVEDAD, Instant.parse("2025-12-10T12:00:00Z")),
                new PublicacionSemilla(
                        "Plan Vale: más envases recuperados en Rivera y Tranqueras",
                        "Convenio con la industria de envases.",
                        "Cuerpo completo en Markdown…", null, "Intendencia de Rivera",
                        TipoPublicacion.NOVEDAD, Instant.parse("2025-11-01T12:00:00Z")),
                new PublicacionSemilla(
                        "Rivera recicla desde el microcentro",
                        "54 comercios separan residuos con el Sello Rivera Verde.",
                        "Cuerpo completo en Markdown…", null, "Renacer del Norte",
                        TipoPublicacion.HISTORIA, Instant.parse("2025-03-31T12:00:00Z")),
                new PublicacionSemilla(
                        "Novo Horizonte estrena su centro de reciclaje",
                        "Nueva sede en el barrio Jardins de Livramento.",
                        "Cuerpo completo en Markdown…", null, "Novo Horizonte",
                        TipoPublicacion.HISTORIA, Instant.parse("2024-11-13T12:00:00Z"))
        ).forEach(app.comunidad::semilla);

        // cuestionarios/ciudadano-es.txt y organizacion-es.txt
        app.cuestionarios.put(Rol.CIUDADANO, Cuestionario.leer("""
                titulo: Cuestionario de Usabilidad - Ciudadano
                opcion: ¿Cuál es tu localidad? | Rivera | Sant'Ana do Livramento.
                escala: ¿Le resultó claro cómo empezar a usar el sistema?
                escala: ¿Le resultó fácil elegir un centro de acopio y consultar sus horarios?
                escala: ¿Le resultó fácil seleccionar los materiales que deseaba reciclar?
                escala: ¿Pudo crear una solicitud de recolección?
                escala: ¿Pudo visualizar el estado de sus solicitudes de forma clara?
                escala: ¿La información sobre sus solicitudes (fecha, estado, materiales) le pareció completa?
                escala: ¿Pudo cancelar una solicitud pendiente sin dificultades?
                escala: ¿Entendió claramente qué acciones podía realizar dentro del sistema?
                escala: ¿La información presentada le resultó clara y comprensible?
                escala: ¿El diseño general le pareció agradable y bien organizado?
                escala: ¿Los tiempos de respuesta fueron adecuados para sus actividades?
                escala: ¿El sistema respondió de forma estable, sin errores o interrupciones frecuentes?
                escala: ¿Le resultó útil poder usar el sistema en su idioma?
                escala: ¿Está satisfecho con su experiencia general de uso del sistema?
                texto: ¿Qué mejoraría o agregaría al sistema?
                """.lines().toList()));
        app.cuestionarios.put(Rol.ORGANIZACION, Cuestionario.leer("""
                titulo: Cuestionario de Usabilidad - Centro de acopio
                opcion: ¿Cuál es tu localidad? | Rivera | Sant'Ana do Livramento
                escala: ¿Le resultó claro cómo elegir el perfil Organización y su centro de acopio?
                escala: ¿Pudo ver las solicitudes recibidas por su centro?
                escala: ¿La organización de las solicitudes por columnas (pendientes, en curso, cerradas) le resultó clara?
                escala: ¿Pudo ver los detalles completos de cada solicitud (materiales, dirección y ubicación)?
                escala: ¿La información de contacto de los usuarios fue accesible cuando la necesitó?
                escala: ¿Le resultó fácil cambiar el estado de una solicitud (aceptar, completar, rechazar)?
                escala: ¿Pudo gestionar múltiples solicitudes de forma eficiente?
                escala: ¿El resumen semanal y el informe mensual le resultaron útiles?
                escala: ¿Pudo actualizar la información de su organización correctamente?
                escala: ¿La información presentada le resultó clara y comprensible?
                escala: ¿El diseño general le pareció agradable y bien organizado?
                escala: ¿Los tiempos de respuesta fueron adecuados para sus actividades?
                escala: ¿El sistema respondió de forma estable, sin errores o interrupciones frecuentes?
                escala: ¿Le resultó útil poder usar el sistema en su idioma?
                escala: ¿Está satisfecho con su experiencia general de uso del sistema?
                texto: ¿Qué mejoraría o agregaría al sistema?
                """.lines().toList()));
    }

    // ======================================================================
    // Capa de consola: la app "usada" desde la terminal
    // ======================================================================
    // Una Pantalla es lo que se pinta: título, líneas del cuerpo (formularios
    // con [ campos ], radios (•), checks [x], botones [ Enviar ]) y la lista
    // numerada de acciones. Escribir un número = pulsar ese botón/enlace.
    record Opcion(String etiqueta, Supplier<Pantalla> ir) { }

    record Pantalla(String titulo, List<String> lineas, List<Opcion> opciones,
            Supplier<Pantalla> volver) { }

    static class Ui {
        private static final int ANCHO = 96;
        private final App app;
        private final Scanner in;
        private final java.util.Queue<String> guion;

        Ui(App app) {
            this.app = app;
            this.in = new Scanner(System.in);
            this.guion = null;
        }
        Ui(App app, List<String> guion) {
            this.app = app;
            this.in = null;
            this.guion = new java.util.ArrayDeque<>(guion);
        }

        private String leer() { return leer(null); }
        private String leer(String prompt) {
            System.out.print(prompt == null ? "> " : "  " + prompt + ": ");
            String linea;
            if (guion != null) {
                linea = guion.isEmpty() ? "0" : guion.poll();
                System.out.println(linea);
                return linea;
            }
            if (in == null || !in.hasNextLine()) {
                System.out.println("0");
                return "0";
            }
            return in.nextLine().strip();
        }

        void ejecutar() {
            var s = new Sesion("10.0.0.1");
            Pantalla p = inicio(s, null);
            while (p != null) {
                pintar(s, p);
                String cmd = leer();
                if (cmd.equalsIgnoreCase("q")) {
                    return;
                }
                int n;
                try {
                    n = Integer.parseInt(cmd);
                } catch (NumberFormatException e) {
                    continue;
                }
                if (n == 0) {
                    p = p.volver() == null ? null : p.volver().get();
                } else if (n >= 1 && n <= p.opciones().size()) {
                    p = p.opciones().get(n - 1).ir().get();
                }
            }
        }

        private void pintar(Sesion s, Pantalla p) {
            var linea = "─".repeat(ANCHO);
            System.out.println("\n" + linea);
            var derecha = s.autenticado
                    ? s.usuario() + campana(s)
                    : "[ Ingresar ]  [ Crear cuenta ]";
            int relleno = Math.max(1, ANCHO - p.titulo().length() - derecha.length() - 2);
            System.out.println(" " + p.titulo() + " ".repeat(relleno) + derecha);
            System.out.println(linea);
            p.lineas().forEach(l -> System.out.println(" " + l));
            System.out.println(linea);
            var sb = new StringBuilder(" ");
            for (int i = 0; i < p.opciones().size(); i++) {
                var trozo = "[" + (i + 1) + "] " + p.opciones().get(i).etiqueta() + "    ";
                if (sb.length() + trozo.length() > ANCHO) {
                    System.out.println(sb);
                    sb = new StringBuilder(" ");
                }
                sb.append(trozo);
            }
            sb.append("[0] ").append(p.volver() == null ? "Salir" : "Volver");
            System.out.println(sb);
        }

        private String campana(Sesion s) {
            long n = app.avisos.avisosSinLeer(s.actor.actual());
            return n == 0 ? "" : "  🔔 " + n;
        }
        private String campanaTxt(Sesion s) {
            long n = app.avisos.avisosSinLeer(s.actor.actual());
            return n == 0 ? "Avisos" : "Avisos (" + n + ")";
        }

        // ---------- piezas de formulario ----------

        private static String radio(boolean marcado) { return marcado ? "(•)" : "( )"; }
        private static String check(boolean marcado) { return marcado ? "[x]" : "[ ]"; }
        private static void campo(List<String> l, String etiqueta, String valor) {
            var celda = valor == null || valor.isEmpty() ? "" : valor;
            l.add(String.format("  %-11s [ %s%s ]", etiqueta, celda,
                    "_".repeat(Math.max(0, 34 - celda.length()))));
        }
        private static void err(List<String> l, Map<String, String> errores, String campo) {
            var msg = errores.get(campo);
            if (msg != null) {
                l.add("      ⚠ " + msg);
            }
        }
        // editar un campo limpia su error, como la validación al salir (blur)
        private Opcion editar(String etiqueta, String campoErr,
                Map<String, String> errores, Consumer<String> poner,
                Supplier<Pantalla> volver) {
            return new Opcion("editar " + etiqueta, () -> {
                poner.accept(leer(etiqueta));
                if (errores != null) {
                    errores.remove(campoErr);
                }
                return volver.get();
            });
        }
        private Pantalla error(String texto, Supplier<Pantalla> volver) {
            return new Pantalla("Error", List.of("  " + texto), List.of(), volver);
        }
        private String barra(int proporcion) {
            return "█".repeat(proporcion / 5) + "░".repeat(20 - proporcion / 5);
        }

        // ---------- pantallas ----------

        Pantalla inicio(Sesion s, String aviso) {
            var l = new ArrayList<String>();
            if (aviso != null) {
                l.add("  ✔ " + aviso);
                l.add("");
            }
            l.add("  Retiros de reciclables a domicilio — Rivera ↔ Sant'Ana do Livramento");
            l.add("");
            var ops = new ArrayList<Opcion>();
            if (!s.autenticado) {
                ops.add(new Opcion("Ingresar", () -> entrar(s, new FormEntrar(), null)));
                ops.add(new Opcion("Crear cuenta", () -> registrar(s, new FormRegistro(), null)));
            } else if (s.actor.actual().esOrganizacion()) {
                ops.add(new Opcion("Solicitudes recibidas", () -> orgBandeja(s, null)));
                ops.add(new Opcion("Datos de mi centro",
                        () -> perfil(s, perfilDe(s), null, null)));
                ops.add(new Opcion(campanaTxt(s), () -> avisos(s)));
            } else {
                ops.add(new Opcion("Pedir un retiro",
                        () -> nueva(s, new FormNueva(s, app.cuentas), null)));
                ops.add(new Opcion("Mis solicitudes",
                        () -> misSolicitudes(s, Filtro.TODAS, 1, null)));
                ops.add(new Opcion(campanaTxt(s), () -> avisos(s)));
            }
            ops.add(new Opcion("Centros de acopio", () -> acopios(s)));
            ops.add(new Opcion("Comunidad", () -> comunidad(s, FiltroComunidad.TODAS)));
            ops.add(new Opcion("Estadísticas", () -> estadisticas(s, Periodo.MES)));
            if (s.autenticado) {
                ops.add(new Opcion("Opinión",
                        () -> opinion(s, new LinkedHashMap<>(), null)));
                ops.add(new Opcion("Cerrar sesión", () -> {
                    app.postSalir(s);
                    return inicio(s, "Sesión cerrada");
                }));
            }
            return new Pantalla("EcoSolicitud · Inicio", l, ops, null);
        }

        // ----- entrar / registrar -----

        static class FormEntrar {
            String usuario = "";
            String clave = "";
            String error;
        }

        Pantalla entrar(Sesion s, FormEntrar f, String aviso) {
            var l = new ArrayList<String>();
            if (aviso != null) {
                l.add("  " + aviso);
            }
            l.add("  Ingresá con tu nombre de usuario y tu contraseña");
            l.add("");
            campo(l, "Usuario", f.usuario);
            campo(l, "Contraseña", "•".repeat(f.clave.length()));
            if (f.error != null) {
                l.add("      ⚠ " + f.error);
            }
            l.add("");
            l.add("        [ Entrar ]");
            var ops = new ArrayList<Opcion>(List.of(
                    editar("Usuario", "usuario", null, v -> f.usuario = v,
                            () -> entrar(s, f, null)),
                    editar("Contraseña", "clave", null, v -> f.clave = v,
                            () -> entrar(s, f, null)),
                    new Opcion("Entrar", () -> {
                        var r = app.postEntrar(s, f.usuario, f.clave);
                        if (r.estado() == 429) {
                            f.error = r.cuerpo();
                            return entrar(s, f, null);
                        }
                        if (r.redirect() != null && r.redirect().contains("?error")) {
                            f.error = "Usuario o contraseña incorrectos";
                            return entrar(s, f, null);
                        }
                        return s.actor.actual().esOrganizacion()
                                ? orgBandeja(s, null)
                                : misSolicitudes(s, Filtro.TODAS, 1, null);
                    }),
                    new Opcion("Crear cuenta", () -> registrar(s, new FormRegistro(), null))));
            return new Pantalla("Ingresar", l, ops, () -> inicio(s, null));
        }

        static class FormRegistro {
            String nombre = "";
            Pais pais = Pais.UY;
            String telefono = "";
            String clave = "";
            boolean condiciones;
            Map<String, String> errores = new LinkedHashMap<>();
        }

        Pantalla registrar(Sesion s, FormRegistro f, String aviso) {
            var l = new ArrayList<String>();
            l.add("  Creá tu cuenta para pedir retiros");
            l.add("");
            campo(l, "Usuario", f.nombre);
            err(l, f.errores, "nombre");
            l.add("  País        " + radio(f.pais == Pais.UY) + " Uruguay (+598)   "
                    + radio(f.pais == Pais.BR) + " Brasil (+55)");
            err(l, f.errores, "pais");
            campo(l, "Celular", f.telefono);
            err(l, f.errores, "telefono");
            campo(l, "Contraseña", "•".repeat(f.clave.length()));
            err(l, f.errores, "clave");
            l.add("  " + check(f.condiciones) + " Acepto las condiciones de uso");
            err(l, f.errores, "condiciones");
            l.add("");
            l.add("        [ Crear cuenta ]");
            var ops = new ArrayList<Opcion>(List.of(
                    editar("Usuario", "nombre", f.errores, v -> f.nombre = v,
                            () -> registrar(s, f, null)),
                    new Opcion("País: " + (f.pais == Pais.UY ? "Uruguay" : "Brasil"), () -> {
                        f.pais = f.pais == Pais.UY ? Pais.BR : Pais.UY;
                        f.errores.remove("pais");
                        return registrar(s, f, null);
                    }),
                    editar("Celular", "telefono", f.errores, v -> f.telefono = v,
                            () -> registrar(s, f, null)),
                    editar("Contraseña", "clave", f.errores, v -> f.clave = v,
                            () -> registrar(s, f, null)),
                    new Opcion((f.condiciones ? "desmarcar" : "aceptar") + " condiciones",
                            () -> {
                                f.condiciones = !f.condiciones;
                                f.errores.remove("condiciones");
                                return registrar(s, f, null);
                            }),
                    new Opcion("Crear cuenta", () -> {
                        var r = app.postRegistrar(s, f.nombre, f.pais.name(), f.telefono,
                                f.clave, f.condiciones ? "on" : null);
                        if (!r.errores().isEmpty()) {
                            f.errores = new LinkedHashMap<>(r.errores());
                            return registrar(s, f, null);
                        }
                        return misSolicitudes(s, Filtro.TODAS, 1, r.cuerpo());
                    })));
            return new Pantalla("Crear cuenta", l, ops, () -> inicio(s, null));
        }

        // ----- nueva solicitud -----

        static class FormNueva {
            Ciudad ciudad = Ciudad.RIVERA;
            String organizacionId;
            Set<Material> materiales = new java.util.LinkedHashSet<>();
            String nombre;
            String contacto = "";
            String direccion = "";
            String referencia = "";
            String nota = "";
            boolean conUbicacion;
            Map<String, String> errores = new LinkedHashMap<>();
            FormNueva(Sesion s, Cuentas cuentas) {
                nombre = s.usuario(); // la app lo precarga con el nombre del actor
                // y ahora también el celular de la cuenta como contacto inicial
                contacto = s.autenticado && s.actor.actual().esCiudadano()
                        ? cuentas.telefonoDe(s.actor.actual().ciudadanoId()).orElse("")
                        : "";
            }
        }

        Pantalla nueva(Sesion s, FormNueva f, String aviso) {
            var centros = app.centrosDe(f.ciudad);
            var centro = centros.stream()
                    .filter(o -> o.id().equals(f.organizacionId)).findFirst();
            var delCentro = centro.map(o -> o.materiales().stream()
                    .filter(Material::isActivo).toList()).orElse(List.<Material>of());
            f.materiales.retainAll(delCentro); // misma regla que NuevaController

            var l = new ArrayList<String>();
            if (aviso != null) {
                l.add("  ✔ " + aviso);
            }
            l.add("  ¿Qué retiramos?");
            l.add("");
            l.add("  Ciudad");
            l.add("    " + radio(f.ciudad == Ciudad.RIVERA) + " Rivera    "
                    + radio(f.ciudad == Ciudad.LIVRAMENTO) + " Sant'Ana do Livramento");
            err(l, f.errores, "ciudad");
            l.add("");
            l.add("  Centro de acopio");
            if (centros.isEmpty()) {
                l.add("    (todavía no hay centros en esta ciudad)");
            }
            for (var c : centros) {
                l.add("    " + radio(c.id().equals(f.organizacionId)) + " " + c.nombre()
                        + "   " + c.horario());
            }
            err(l, f.errores, "organizacionId");
            l.add("");
            l.add("  Materiales " + (centro.isEmpty() ? "(elegí un centro primero)" : ""));
            var sb = new StringBuilder("    ");
            for (var m : delCentro) {
                sb.append(check(f.materiales.contains(m))).append(' ')
                        .append(m.getNombre()).append("    ");
            }
            l.add(sb.toString());
            err(l, f.errores, "materiales");
            l.add("");
            campo(l, "Nombre", f.nombre);
            err(l, f.errores, "nombre");
            campo(l, "Contacto", f.contacto);
            err(l, f.errores, "contacto");
            campo(l, "Dirección", f.direccion);
            err(l, f.errores, "direccion");
            campo(l, "Referencia", f.referencia);
            err(l, f.errores, "referencia");
            campo(l, "Nota", f.nota);
            err(l, f.errores, "nota");
            l.add("  Ubicación   " + (f.conUbicacion
                    ? "📍 marcada en el mapa" : "sin marcar (opcional)"));
            err(l, f.errores, "latitud");
            l.add("");
            l.add("        [ Enviar pedido ]");

            var ops = new ArrayList<Opcion>();
            ops.add(new Opcion("cambiar ciudad", () -> {
                f.ciudad = f.ciudad == Ciudad.RIVERA ? Ciudad.LIVRAMENTO : Ciudad.RIVERA;
                f.organizacionId = null;
                f.materiales.clear();
                f.errores.keySet().retainAll(List.of());
                return nueva(s, f, null);
            }));
            for (var c : centros) {
                ops.add(new Opcion("centro: " + c.nombre(), () -> {
                    f.organizacionId = c.id();
                    f.errores.remove("organizacionId");
                    f.errores.remove("materiales");
                    return nueva(s, f, null);
                }));
            }
            for (var m : delCentro) {
                ops.add(new Opcion((f.materiales.contains(m) ? "desmarcar " : "marcar ")
                        + m.getNombre(), () -> {
                            if (!f.materiales.remove(m)) {
                                f.materiales.add(m);
                            }
                            f.errores.remove("materiales");
                            return nueva(s, f, null);
                        }));
            }
            ops.add(editar("Nombre", "nombre", f.errores, v -> f.nombre = v,
                    () -> nueva(s, f, null)));
            ops.add(editar("Contacto", "contacto", f.errores, v -> f.contacto = v,
                    () -> nueva(s, f, null)));
            ops.add(editar("Dirección", "direccion", f.errores, v -> f.direccion = v,
                    () -> nueva(s, f, null)));
            ops.add(editar("Referencia", "referencia", f.errores, v -> f.referencia = v,
                    () -> nueva(s, f, null)));
            ops.add(editar("Nota", "nota", f.errores, v -> f.nota = v,
                    () -> nueva(s, f, null)));
            ops.add(new Opcion(f.conUbicacion ? "quitar ubicación" : "usar mi ubicación",
                    () -> {
                        f.conUbicacion = !f.conUbicacion;
                        f.errores.remove("latitud");
                        return nueva(s, f, null);
                    }));
            ops.add(new Opcion("Enviar pedido", () -> {
                Double lat = null, lon = null;
                if (f.conUbicacion) {
                    lat = f.ciudad == Ciudad.RIVERA ? -30.905 : -30.890;
                    lon = f.ciudad == Ciudad.RIVERA ? -55.551 : -55.532;
                }
                var r = app.postNueva(s, f.ciudad, List.copyOf(f.materiales),
                        f.organizacionId, f.nombre, f.contacto, f.direccion,
                        f.referencia, lat, lon, f.nota);
                if (!r.errores().isEmpty()) {
                    f.errores = new LinkedHashMap<>(r.errores());
                    return nueva(s, f, null);
                }
                return misSolicitudes(s, Filtro.TODAS, 1, r.cuerpo());
            }));
            return new Pantalla("Pedir un retiro", l, ops, () -> inicio(s, null));
        }

        // ----- mis solicitudes -----

        Pantalla misSolicitudes(Sesion s, Filtro filtro, int pagina, String aviso) {
            var actor = s.actor.actual();
            var p = app.listados.misSolicitudes(actor.ciudadanoId(), filtro, pagina);
            var l = new ArrayList<String>();
            if (aviso != null) {
                l.add("  ✔ " + aviso);
                l.add("");
            }
            var semana = app.listados.semana(actor);
            l.add(String.format("  Esta semana: %d pedidas · %d pendientes",
                    semana.recibidas(), semana.pendientes()));
            l.add("  Filtro: " + check(filtro == Filtro.TODAS) + " Todas   "
                    + check(filtro == Filtro.ABIERTAS) + " Abiertas   "
                    + check(filtro == Filtro.CERRADAS) + " Cerradas");
            l.add("");
            if (p.contenido().isEmpty()) {
                l.add("  (todavía no hay solicitudes)");
            }
            var ops = new ArrayList<Opcion>();
            for (var i : p.contenido()) {
                l.add("  #" + i.id() + "  " + i.estado().etiqueta() + "   → "
                        + i.organizacionNombre());
                l.add("      " + i.materiales().stream().map(Material::getNombre)
                        .collect(Collectors.joining(", ")) + " · creada " + i.creada()
                        + (i.finalizadaEn() != null ? " · cerrada " + i.finalizada() : ""));
                var chips = new StringBuilder("      [ Detalles ]");
                if (i.estado().permiteCancelar()) {
                    chips.append("  [ Cancelar ]");
                }
                l.add(chips.toString());
                l.add("");
                ops.add(new Opcion("Detalles #" + i.id(),
                        () -> detalle(s, i.id(), () -> misSolicitudes(s, filtro, pagina, null))));
                if (i.estado().permiteCancelar()) {
                    ops.add(new Opcion("Cancelar #" + i.id(), () -> {
                        var r = app.solicitudes.cancelar(actor, i.id(), i.version());
                        return misSolicitudes(s, filtro, pagina, app.textoResultado(r,
                                "Solicitud #" + i.id() + " cancelada · quedó sin efecto",
                                i.id()));
                    }));
                }
            }
            l.add("  página " + p.numero() + " de " + p.totalPaginas()
                    + " (" + p.totalElementos() + " solicitudes)");
            for (var f : Filtro.values()) {
                if (f != filtro) {
                    var f2 = f;
                    ops.add(new Opcion("filtro " + f2.name().toLowerCase(),
                            () -> misSolicitudes(s, f2, 1, null)));
                }
            }
            if (p.anterior()) {
                ops.add(new Opcion("‹ página anterior",
                        () -> misSolicitudes(s, filtro, pagina - 1, null)));
            }
            if (p.siguiente()) {
                ops.add(new Opcion("página siguiente ›",
                        () -> misSolicitudes(s, filtro, pagina + 1, null)));
            }
            ops.add(new Opcion("Informe del mes",
                    () -> informe(s, YearMonth.now(), () -> misSolicitudes(s, filtro, pagina, null))));
            return new Pantalla("Mis solicitudes", l, ops, () -> inicio(s, null));
        }

        Pantalla detalle(Sesion s, long id, Supplier<Pantalla> origen) {
            var actor = s.actor.actual();
            var encontrada = app.solicitudes.detalle(actor, id);
            if (encontrada.isEmpty()) {
                return error("404 · No encontrada (no es tuya ni de tu centro)", origen);
            }
            var i = encontrada.get();
            var l = new ArrayList<String>();
            l.add("  Solicitud #" + i.id() + "   " + i.estado().etiqueta());
            l.add("");
            l.add("  Centro:     " + i.organizacionNombre());
            l.add("  Vecino:     " + i.nombreCiudadano() + "   contacto " + i.contacto());
            l.add("  Materiales: " + i.materiales().stream().map(Material::getNombre)
                    .collect(Collectors.joining(", ")));
            l.add("  Dirección:  " + i.direccion()
                    + (i.referencia() != null ? " (" + i.referencia() + ")" : ""));
            if (i.ubicacion() != null) {
                l.add("  Mapa:       " + i.ubicacion().enlaceMapa());
            }
            if (i.nota() != null && !i.nota().isBlank()) {
                l.add("  Nota:       " + i.nota());
            }
            l.add("  Creada:     " + i.creada()
                    + (i.finalizadaEn() != null ? "   Cerrada: " + i.finalizada() : ""));
            l.add("");
            var ops = new ArrayList<Opcion>();
            var e = i.estado();
            if (actor.esCiudadano() && e.permiteCancelar()) {
                ops.add(new Opcion("Cancelar", () -> {
                    var r = app.solicitudes.cancelar(actor, i.id(), i.version());
                    return origen.get() == null ? null
                            : conAviso(origen.get(), app.textoResultado(r,
                                    "Solicitud #" + i.id() + " cancelada · quedó sin efecto",
                                    i.id()));
                }));
            }
            if (actor.esOrganizacion()) {
                var orgId = actor.organizacionId();
                if (e.permiteAceptar()) {
                    ops.add(new Opcion("Aceptar", () -> accionOrg(s, i, "aceptar", orgId, origen)));
                    ops.add(new Opcion("Rechazar", () -> accionOrg(s, i, "rechazar", orgId, origen)));
                } else if (e.permiteCompletar()) {
                    ops.add(new Opcion("Completar", () -> accionOrg(s, i, "completar", orgId, origen)));
                    ops.add(new Opcion("Rechazar", () -> accionOrg(s, i, "rechazar", orgId, origen)));
                }
            }
            return new Pantalla("Solicitud #" + id, l, ops, origen);
        }

        private Pantalla accionOrg(Sesion s, SolicitudInfo i, String accion,
                String orgId, Supplier<Pantalla> origen) {
            var actor = s.actor.actual();
            var r = switch (accion) {
                case "aceptar" -> app.solicitudes.aceptar(actor, i.id(), i.version());
                case "rechazar" -> app.solicitudes.rechazar(actor, i.id(), i.version());
                default -> app.solicitudes.completar(actor, i.id(), i.version());
            };
            var ok = switch (accion) {
                case "aceptar" -> "Solicitud #" + i.id() + " aceptada · ya avisamos al vecino";
                case "rechazar" -> "Solicitud #" + i.id() + " rechazada · el vecino ya lo sabe";
                default -> "Solicitud #" + i.id() + " completada · el material ya llegó al centro";
            };
            return conAviso(origen.get(), app.textoResultado(r, ok, i.id()));
        }

        private Pantalla conAviso(Pantalla p, String aviso) {
            var lineas = new ArrayList<String>();
            lineas.add("  ✔ " + aviso);
            lineas.add("");
            lineas.addAll(p.lineas());
            return new Pantalla(p.titulo(), lineas, p.opciones(), p.volver());
        }

        // ----- bandeja de la organización -----

        Pantalla orgBandeja(Sesion s, String aviso) {
            var actor = s.actor.actual();
            var org = app.organizaciones.actual(actor);
            var bandeja = app.listados.recibidas(actor);
            var l = new ArrayList<String>();
            if (aviso != null) {
                l.add("  ✔ " + aviso);
                l.add("");
            }
            l.add("  " + org.nombre() + " — " + org.ciudad().nombre());
            var semana = app.listados.semana(actor);
            l.add(String.format(
                    "  Semana: %d recibidas · aceptación %s · resolución %s · espera %s",
                    semana.recibidas(), semana.aceptacion(), semana.resolucion(),
                    semana.espera()));
            l.add("");
            var ops = new ArrayList<Opcion>();
            for (var grupo : bandeja.grupos()) {
                l.add("  ── " + Mensajes.t(grupo.clave()) + " ("
                        + grupo.solicitudes().size() + " de " + grupo.total() + ")");
                for (var i : grupo.solicitudes()) {
                    l.add("    #" + i.id() + "  " + i.nombreCiudadano() + "   "
                            + i.materiales().stream().map(Material::getNombre)
                                    .collect(Collectors.joining(", "))
                            + "   " + i.creada());
                    var chips = new StringBuilder("        ");
                    var e = i.estado();
                    if (e.permiteAceptar()) {
                        chips.append("[ Aceptar ]  [ Rechazar ]  ");
                    } else if (e.permiteCompletar()) {
                        chips.append("[ Completar ]  [ Rechazar ]  ");
                    }
                    chips.append("[ Detalles ]");
                    l.add(chips.toString());
                    if (e.permiteAceptar()) {
                        ops.add(new Opcion("Aceptar #" + i.id(),
                                () -> accionOrg(s, i, "aceptar", null,
                                        () -> orgBandeja(s, null))));
                        ops.add(new Opcion("Rechazar #" + i.id(),
                                () -> accionOrg(s, i, "rechazar", null,
                                        () -> orgBandeja(s, null))));
                    } else if (e.permiteCompletar()) {
                        ops.add(new Opcion("Completar #" + i.id(),
                                () -> accionOrg(s, i, "completar", null,
                                        () -> orgBandeja(s, null))));
                        ops.add(new Opcion("Rechazar #" + i.id(),
                                () -> accionOrg(s, i, "rechazar", null,
                                        () -> orgBandeja(s, null))));
                    }
                    ops.add(new Opcion("Detalles #" + i.id(),
                            () -> detalle(s, i.id(), () -> orgBandeja(s, null))));
                }
                l.add("");
            }
            l.add("  Archivadas: " + app.listados.totalArchivadas(actor)
                    + "   [ Informe del mes ]   [ Ver archivadas ]   [ Copia .txt ]");
            ops.add(new Opcion("Informe del mes",
                    () -> informe(s, YearMonth.now(), () -> orgBandeja(s, null))));
            ops.add(new Opcion("Ver archivadas", () -> orgArchivadas(s, 1)));
            ops.add(new Opcion("Copia .txt", () -> copia(s)));
            return new Pantalla("Solicitudes recibidas", l, ops, () -> inicio(s, null));
        }

        Pantalla orgArchivadas(Sesion s, int pagina) {
            var actor = s.actor.actual();
            var p = app.listados.archivadas(actor, pagina);
            var l = new ArrayList<String>();
            for (var i : p.contenido()) {
                l.add("  #" + i.id() + "  " + i.estado().etiqueta() + "   "
                        + i.nombreCiudadano() + "   cerrada " + i.finalizada());
            }
            if (p.contenido().isEmpty()) {
                l.add("  (sin archivadas)");
            }
            l.add("  página " + p.numero() + " de " + p.totalPaginas());
            var ops = new ArrayList<Opcion>();
            if (p.anterior()) {
                ops.add(new Opcion("‹ anterior", () -> orgArchivadas(s, pagina - 1)));
            }
            if (p.siguiente()) {
                ops.add(new Opcion("siguiente ›", () -> orgArchivadas(s, pagina + 1)));
            }
            return new Pantalla("Archivadas", l, ops, () -> orgBandeja(s, null));
        }

        Pantalla informe(Sesion s, YearMonth mes, Supplier<Pantalla> origen) {
            var informe = app.listados.informe(s.actor.actual(), mes);
            var l = new ArrayList<String>();
            l.add("  Informe de " + mes);
            l.add("");
            l.add("  Por estado:");
            for (var e : Estado.values()) {
                var n = informe.porEstado().getOrDefault(e, 0L);
                l.add(String.format("    %-11s %2d  %s", e.etiqueta(), n,
                        barra((int) Math.min(100, n * 20))));
            }
            l.add("");
            l.add("  Por material: " + informe.porMaterial());
            l.add("  Solicitudes: " + informe.solicitudes().size());
            var ops = new ArrayList<Opcion>(List.of(
                    new Opcion("‹ mes anterior",
                            () -> informe(s, mes.minusMonths(1), origen)),
                    new Opcion("mes siguiente ›",
                            () -> informe(s, mes.plusMonths(1), origen))));
            return new Pantalla("Informe " + mes, l, ops, origen);
        }

        Pantalla copia(Sesion s) {
            var r = app.getOrgCopia(s);
            var l = new ArrayList<String>();
            l.add("  ecosolicitud-" + LocalDate.now(Fechas.ZONA) + ".txt (descargada)");
            l.add("");
            if (r.cuerpo() != null) {
                l.addAll(r.cuerpo().lines().map(x -> "  " + x).toList());
            }
            return new Pantalla("Copia de respaldo", l, List.of(),
                    () -> orgBandeja(s, null));
        }

        // ----- avisos -----

        Pantalla avisos(Sesion s) {
            var actor = s.actor.actual();
            var lista = app.avisos.avisosPara(actor);
            var l = new ArrayList<String>();
            if (lista.isEmpty()) {
                l.add("  (sin avisos)");
            }
            var ops = new ArrayList<Opcion>();
            for (var a : lista) {
                l.add("  " + (a.leida() ? "  " : "● ") + textoAvisoConsola(a)
                        + "   " + Fechas.corta(a.creadaEn())
                        + "   (hoy: " + a.estado().etiqueta() + ")");
                if (a.solicitudId() != null) {
                    long id = a.solicitudId();
                    ops.add(new Opcion("Ver #" + id,
                            () -> detalle(s, id, () -> avisosMarcadas(s))));
                }
            }
            if (actor.esOrganizacion()) {
                l.add("");
                for (var c : app.avisos.kanbanPara(actor)) {
                    l.add("  " + Mensajes.t(c.clave()) + ": " + c.avisos().size());
                }
            }
            app.avisos.marcarLeidas(actor); // abrir la bandeja las marca
            return new Pantalla("Avisos", l, ops, () -> inicio(s, null));
        }

        // al volver del detalle no re-marcar ni re-listar de nuevo: mismas leídas
        private Pantalla avisosMarcadas(Sesion s) {
            return avisos(s);
        }

        private static String textoAvisoConsola(AvisoInfo a) {
            return Mensajes.t(a.tipo().getClave(), a.detalle())
                    + " (#" + a.solicitudId() + ")";
        }

        // ----- perfil de la organización -----

        static class FormPerfil {
            Set<Material> materiales = new java.util.LinkedHashSet<>();
            String horario = "";
            String telefono = "";
            boolean conUbicacion;
            long version;
            Map<String, String> errores = new LinkedHashMap<>();
        }

        private FormPerfil perfilDe(Sesion s) {
            var org = app.organizaciones.actual(s.actor.actual());
            var f = new FormPerfil();
            f.materiales.addAll(org.materiales());
            f.horario = org.horario();
            f.telefono = org.telefono();
            f.conUbicacion = org.ubicacion() != null;
            f.version = org.version();
            return f;
        }

        Pantalla perfil(Sesion s, FormPerfil f, String aviso, String extra) {
            var l = new ArrayList<String>();
            if (aviso != null) {
                l.add("  ✔ " + aviso);
            }
            if (extra != null) {
                l.add("  ⚠ " + extra);
            }
            l.add("  Qué recibe tu centro y cómo contactarte");
            l.add("");
            l.add("  Materiales que recibimos");
            var sb = new StringBuilder("    ");
            for (var m : app.catalogo.activos()) {
                sb.append(check(f.materiales.contains(m))).append(' ')
                        .append(m.getNombre()).append("    ");
            }
            l.add(sb.toString());
            err(l, f.errores, "materiales");
            campo(l, "Horario", f.horario);
            err(l, f.errores, "horario");
            campo(l, "Teléfono", f.telefono);
            err(l, f.errores, "telefono");
            l.add("  Ubicación   " + (f.conUbicacion
                    ? "📍 marcada en el mapa" : "sin marcar"));
            err(l, f.errores, "latitud");
            l.add("");
            l.add("        [ Guardar cambios ]");
            var ops = new ArrayList<Opcion>();
            for (var m : app.catalogo.activos()) {
                ops.add(new Opcion((f.materiales.contains(m) ? "desmarcar " : "marcar ")
                        + m.getNombre(), () -> {
                            if (!f.materiales.remove(m)) {
                                f.materiales.add(m);
                            }
                            f.errores.remove("materiales");
                            return perfil(s, f, null, null);
                        }));
            }
            ops.add(editar("Horario", "horario", f.errores, v -> f.horario = v,
                    () -> perfil(s, f, null, null)));
            ops.add(editar("Teléfono", "telefono", f.errores, v -> f.telefono = v,
                    () -> perfil(s, f, null, null)));
            ops.add(new Opcion(f.conUbicacion ? "quitar ubicación" : "usar mi ubicación",
                    () -> {
                        f.conUbicacion = !f.conUbicacion;
                        f.errores.remove("latitud");
                        return perfil(s, f, null, null);
                    }));
            ops.add(new Opcion("Guardar cambios", () -> {
                Double lat = null, lon = null;
                if (f.conUbicacion) {
                    var org = app.organizaciones.actual(s.actor.actual());
                    lat = org.ubicacion() == null ? -30.912 : org.ubicacion().latitud();
                    lon = org.ubicacion() == null ? -55.556 : org.ubicacion().longitud();
                }
                var r = app.postOrgPerfil(s, List.copyOf(f.materiales), f.horario,
                        f.telefono, lat, lon, f.version);
                if (!r.errores().isEmpty()) {
                    f.errores = new LinkedHashMap<>(r.errores());
                    return perfil(s, f, null,
                            f.errores.containsKey("conflicto")
                                    ? f.errores.get("conflicto") : null);
                }
                return perfil(s, perfilDe(s), "Perfil guardado", null);
            }));
            return new Pantalla("Datos de mi centro", l, ops, () -> inicio(s, null));
        }

        // ----- públicas -----

        Pantalla acopios(Sesion s) {
            var l = new ArrayList<String>();
            for (var g : app.organizaciones.porCiudad()) {
                l.add("  " + g.ciudad().nombre());
                for (var o : g.organizaciones()) {
                    l.add("    ▣ " + o.nombre());
                    l.add("      " + o.horario() + " · " + o.telefono());
                    l.add("      recibe: " + o.materiales().stream()
                            .filter(Material::isActivo).map(Material::getNombre)
                            .collect(Collectors.joining(", ")));
                    if (o.ubicacion() != null) {
                        l.add("      " + o.ubicacion().enlaceMapa());
                    }
                }
                l.add("");
            }
            return new Pantalla("Centros de acopio", l, List.of(), () -> inicio(s, null));
        }

        Pantalla comunidad(Sesion s, FiltroComunidad filtro) {
            var l = new ArrayList<String>();
            l.add("  " + check(filtro == FiltroComunidad.TODAS) + " Todas   "
                    + check(filtro == FiltroComunidad.NOVEDAD) + " Novedades   "
                    + check(filtro == FiltroComunidad.HISTORIA) + " Historias");
            l.add("");
            var ops = new ArrayList<Opcion>();
            for (var p : app.comunidad.publicadas(filtro)) {
                l.add("  [" + p.tipo() + "] " + p.titulo());
                l.add("      " + p.resumen());
                l.add("      — " + p.organizacion() + "   " + p.fecha());
                l.add("");
                ops.add(new Opcion("Leer: " + p.titulo(), () -> publicacion(s, p.id())));
            }
            l.add("  Quién recicla por su cuenta:");
            for (var r : app.comunidad.recicladores()) {
                l.add("    • " + r.nombre() + " (" + r.ciudad().nombre() + ")"
                        + (r.telefono() != null ? "  " + r.telefono() : ""));
            }
            for (var f : FiltroComunidad.values()) {
                if (f != filtro) {
                    var f2 = f;
                    ops.add(new Opcion("filtro " + f2.name().toLowerCase(),
                            () -> comunidad(s, f2)));
                }
            }
            return new Pantalla("Comunidad", l, ops, () -> inicio(s, null));
        }

        Pantalla publicacion(Sesion s, long id) {
            var p = app.comunidad.buscar(id);
            if (p.isEmpty()) {
                return error("404 · No encontrada", () -> comunidad(s, FiltroComunidad.TODAS));
            }
            var pub = p.get();
            var l = new ArrayList<String>();
            l.add("  " + pub.titulo());
            l.add("  — " + pub.organizacion() + "   " + pub.fecha());
            l.add("");
            l.addAll(pub.cuerpoHtml().lines().map(x -> "  " + x).toList());
            return new Pantalla("Comunidad · artículo", l, List.of(),
                    () -> comunidad(s, FiltroComunidad.TODAS));
        }

        Pantalla estadisticas(Sesion s, Periodo periodo) {
            var info = app.estadisticas.resumen(periodo);
            var l = new ArrayList<String>();
            var t = info.total();
            l.add(String.format("  %d recibidas · %d respondidas · aceptación %s · "
                    + "resolución %s · mediana %s · espera %s",
                    t.recibidas(), t.respondidas(), t.aceptacion(), t.resolucion(),
                    t.mediana(), t.espera()));
            l.add("");
            l.add("  Por estado");
            for (var b : info.series().porEstado()) {
                l.add(String.format("    %-22s %2d  %s", Mensajes.t(b.clave()), b.valor(),
                        barra(b.proporcion())));
            }
            l.add("");
            l.add("  Por material");
            for (var b : info.series().porMaterial()) {
                l.add(String.format("    %-14s %2d  %s", b.etiqueta(), b.valor(),
                        barra(b.proporcion())));
            }
            l.add("");
            l.add("  En el tiempo");
            for (var b : info.series().enElTiempo()) {
                l.add(String.format("    %-7s %2d  %s", b.etiqueta(), b.valor(),
                        barra(b.proporcion())));
            }
            l.add("");
            for (var c : info.centros()) {
                l.add("  " + c.nombre() + " (" + c.ciudad().nombre() + "): "
                        + c.metricas().recibidas() + " recibidas · aceptación "
                        + c.metricas().aceptacion() + " · resolución "
                        + c.metricas().resolucion());
            }
            var ops = new ArrayList<Opcion>();
            for (var p : Periodo.values()) {
                if (p != periodo) {
                    var p2 = p;
                    ops.add(new Opcion("período " + (p2 == Periodo.MES ? "30d" : "todo"),
                            () -> estadisticas(s, p2)));
                }
            }
            return new Pantalla("Estadísticas · " + (periodo == Periodo.MES ? "últimos 30 días" : "todo el historial"),
                    l, ops, () -> inicio(s, null));
        }

        // ----- opinión -----

        Pantalla opinion(Sesion s, Map<Integer, String> respuestas, String aviso) {
            var c = app.cuestionarios.get(s.actor.get());
            var l = new ArrayList<String>();
            if (aviso != null) {
                l.add("  " + aviso);
            }
            l.add("  " + c.titulo());
            l.add("  (todas las preguntas son opcionales; se envía si respondés al");
            l.add("   menos una escala — igual que el formulario original)");
            l.add("");
            var ops = new ArrayList<Opcion>();
            for (int i = 0; i < c.preguntas().size(); i++) {
                var p = c.preguntas().get(i);
                var r = respuestas.getOrDefault(i, "—");
                l.add(String.format("  %2d. %-70s %s", i + 1,
                        recortar(p.texto(), 70), r));
                int idx = i;
                ops.add(new Opcion("responder #" + (i + 1),
                        () -> preguntaOpinion(s, idx, respuestas)));
            }
            l.add("");
            l.add("        [ Enviar respuestas ]");
            ops.add(new Opcion("Enviar respuestas", () -> {
                var r = app.postOpinionRespaldo(s, respuestas);
                if (r.redirect() != null && r.redirect().contains("gracias")) {
                    return inicio(s, "¡Gracias por tu opinión!");
                }
                return opinion(s, respuestas,
                        "⚠ Revisá las respuestas: falta una escala o algún valor no es válido");
            }));
            return new Pantalla("Tu opinión", l, ops, () -> inicio(s, null));
        }

        Pantalla preguntaOpinion(Sesion s, int i, Map<Integer, String> respuestas) {
            var c = app.cuestionarios.get(s.actor.get());
            var p = c.preguntas().get(i);
            var l = new ArrayList<String>();
            l.add("  " + p.texto());
            l.add("");
            var ops = new ArrayList<Opcion>();
            switch (p.tipo()) {
                case ESCALA -> {
                    l.add("   (1 = muy malo · 5 = muy bueno)");
                    for (int v = 1; v <= 5; v++) {
                        var valor = String.valueOf(v);
                        ops.add(new Opcion(valor, () -> {
                            respuestas.put(i, valor);
                            return opinion(s, respuestas, null);
                        }));
                    }
                }
                case OPCION -> {
                    for (var o : p.opciones()) {
                        ops.add(new Opcion(o, () -> {
                            respuestas.put(i, o);
                            return opinion(s, respuestas, null);
                        }));
                    }
                }
                case TEXTO -> ops.add(new Opcion("escribir respuesta", () -> {
                    respuestas.put(i, leer("Tu respuesta"));
                    return opinion(s, respuestas, null);
                }));
            }
            return new Pantalla("Pregunta " + (i + 1) + " de " + c.preguntas().size(),
                    l, ops, () -> opinion(s, respuestas, null));
        }

        private static String recortar(String texto, int max) {
            return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
        }
    }

    // ======================================================================
    // Escenario: varios usuarios usando la app (modo --log)
    // ======================================================================

    static int paso = 0;

    static void titulo(String texto) {
        System.out.println("\n===== " + texto + " =====");
    }
    static void mostrar(String quien, String accion, Object respuesta) {
        System.out.printf("%-16s %-58s %s%n", quien, accion, respuesta);
    }
    static void nota(String texto) {
        System.out.println("   » " + texto);
    }
    static void bloque(String texto) {
        Arrays.stream(texto.split("\n")).forEach(l -> System.out.println("   " + l));
    }

    public static void main(String[] args) {
        var app = new App();
        sembrar(app);
        // java Simulacion.java          → la app usable desde la consola
        // java Simulacion.java --demo   → el guion de abajo, pintando pantallas
        // java Simulacion.java --log    → traza HTTP del escenario anterior
        if (args.length == 0) {
            new Ui(app).ejecutar();
            return;
        }
        switch (args[0]) {
            case "--demo" -> new Ui(app, guionDemo()).ejecutar();
            case "--log" -> escenarioLog(app);
            default -> System.out.println("Uso: java Simulacion.java [--demo|--log]");
        }
    }

    // El guion del --demo: cada línea es lo que el usuario teclea en el prompt.
    // Opciones numeradas según el orden del menú de cada pantalla.
    static List<String> guionDemo() {
        return List.of(
                // portada: [1] Ingresar [2] Crear cuenta [3] Acopios [4] Comunidad [5] Estadísticas
                "3",          // mira los centros
                "0",          // vuelve
                "2",          // Crear cuenta
                // registrar: [1] usuario [2] país [3] celular [4] clave [5] condiciones [6] Crear
                "1", "ana",           // nombre corto → error
                "6",                  // intenta crear → errores en pantalla
                "1", "anaprueba",
                "3", "099 111 222",
                "4", "clave1",
                "5",                  // acepta condiciones
                "6",                  // → /mis-solicitudes con bienvenida
                // mis solicitudes (vacía): vuelve al inicio
                "0",
                // inicio: [1] Pedir un retiro
                "1",
                // nueva: [1] cambiar ciudad [2] centro Microcentro [3] centro Paso del Enano
                // [4..9] materiales del centro [10] nombre [11] contacto [12] dirección
                // [13] referencia [14] nota [15] ubicación [16] Enviar
                "3",                  // centro: Paso del Enano
                "4",                  // marca Plástico
                "7",                  // marca Vidrio
                "16",                 // enviar → faltan dirección y contacto
                "12", "Av. Italia 1234",
                "11", "099 111 222",
                "13", "Portón rojo",
                "15",                 // usa mi ubicación
                "16",                 // enviar → solicitud creada
                // mis solicitudes con la nueva tarjeta
                "0",
                // inicio ciudadano: [1] Pedir [2] Mis solicitudes [3] Avisos
                // [4] Acopios [5] Comunidad [6] Estadísticas [7] Opinión [8] Cerrar sesión
                "8",
                // vuelve a portada anónima → Ingresar
                "1",
                // entrar: [1] usuario [2] clave [3] Entrar [4] Crear cuenta
                "1", "renacer-norte",
                "2", "centro-demo",
                "3",                  // → bandeja de la org
                // bandeja: pendientes #7 [1]Aceptar [2]Rechazar [3]Detalles;
                // #3 lo mismo [4..6]; en curso #2 [7]Completar [8]Rechazar
                // [9]Detalles; cerrada #1 [10]Detalles; [11]informe [12]arch. [13]copia
                "1",                  // Aceptar #7 → pasa a En curso
                // ahora en curso: #7 [4]Completar
                "4",                  // Completar #7 → cierra
                "0",                  // vuelve al inicio
                // inicio org: [3] Avisos (con ● nuevas)
                "3",                  // bandeja de avisos (las marca leídas)
                "0",
                // [6] Estadísticas
                "6",
                "0",
                // [8] Cerrar sesión → portada anónima
                "8",
                "0");                 // Salir
    }

    static void escenarioLog(App app) {

        titulo("Arranque: catálogo, centros, cuentas de organización y dataset demo");
        nota("Materiales activos: "
                + app.catalogo.activos().stream().map(Material::getCodigo).toList());
        nota("Centros: " + app.organizaciones.todas().stream()
                .map(o -> o.id() + " (" + o.ciudad() + ")").toList());
        nota("Cuentas de centro: renacer-norte → frontera-limpia, "
                + "novo-horizonte → coleta-solidaria (clave 'centro-demo')");
        nota("Solicitudes del dataset: " + app.solicitudes.todas().size());

        // ---------- Sesiones: cada usuario es un navegador distinto ----------
        var ana = new Sesion("10.0.0.1");       // ciudadana nueva, Rivera
        var bruno = new Sesion("10.0.0.2");     // otro ciudadano
        var centroNorte = new Sesion("10.0.0.9"); // cuenta de la organización
        var centroJardins = new Sesion("10.0.0.10");
        var anonimo = new Sesion("10.0.0.99");

        titulo("Acceso anónimo y reglas de acceso (SeguridadConfig)");
        mostrar("anónimo", "GET /nueva", app.getNueva(anonimo, Ciudad.RIVERA, null, null));
        mostrar("anónimo", "GET /mis-solicitudes",
                app.getMisSolicitudes(anonimo, null, 1));
        mostrar("anónimo", "GET /org/solicitudes", app.getOrgSolicitudes(anonimo));
        mostrar("anónimo", "GET /acopios (pública)", "200 — se muestra igual");

        titulo("Registro con errores de validación (Cuentas.validarDatos)");
        mostrar("ana", "POST registrar nombre='ana' (<5)",
                app.postRegistrar(ana, "ana", "UY", "099 111 222", "clave1", "on"));
        mostrar("ana", "POST registrar con símbolos",
                app.postRegistrar(ana, "ana maria", "UY", "099 111 222", "clave1", "on"));
        mostrar("ana", "POST registrar teléfono fijo",
                app.postRegistrar(ana, "anaprueba", "UY", "2400 0000", "clave1", "on"));
        mostrar("ana", "POST registrar sin condiciones",
                app.postRegistrar(ana, "anaprueba", "UY", "099 111 222", "clave1", null));
        mostrar("ana", "POST registrar clave corta",
                app.postRegistrar(ana, "anaprueba", "UY", "099 111 222", "abc", "on"));

        titulo("Registro exitoso = sesión autenticada + redirect al propio panel");
        var r = app.postRegistrar(ana, "anaprueba", "UY", "099 111 222", "clave1", "on");
        mostrar("ana", "POST registrar 'anaprueba'", r);
        nota("autenticada=" + ana.autenticado + ", actor=" + ana.usuario()
                + ", rol=" + ana.actor.get());
        mostrar("bruno", "POST registrar 'brunop' mismo celular",
                app.postRegistrar(bruno, "brunop", "UY", "+598 99 111 222", "clave2", "on"));
        mostrar("bruno", "POST registrar 'brunop' nombre de org",
                app.postRegistrar(bruno, "renacer-norte", "UY", "099 555 000", "clave2", "on"));
        mostrar("bruno", "POST registrar 'brunop' OK",
                app.postRegistrar(bruno, "brunop", "UY", "099 555 000", "clave2", "on"));

        titulo("Login (formLogin: usuario + clave) y límite de intentos");
        var invitado = new Sesion("10.0.0.50");
        mostrar("invitado", "POST entrar clave errada x1",
                app.postEntrar(invitado, "anaprueba", "mala"));
        mostrar("invitado", "x2", app.postEntrar(invitado, "anaprueba", "mala"));
        mostrar("invitado", "x3", app.postEntrar(invitado, "anaprueba", "mala"));
        mostrar("invitado", "x4", app.postEntrar(invitado, "anaprueba", "mala"));
        mostrar("invitado", "x5", app.postEntrar(invitado, "anaprueba", "mala"));
        mostrar("invitado", "x6 → filtro 429",
                app.postEntrar(invitado, "anaprueba", "clave1"));

        titulo("Pedido de retiro: GET /nueva con cascada ciudad→centro→materiales");
        bloque(app.getNueva(ana, Ciudad.RIVERA, "acopio-verde",
                List.of(app.catalogo.resolver("VIDRIO"), app.catalogo.resolver("CARTON")))
                .cuerpo());
        nota("acopio-verde no recibe VIDRIO: el marcado se limpia (retainAll)");

        titulo("POST /nueva con errores por campo");
        mostrar("ana", "sin ciudad", app.postNueva(ana, null,
                List.of(app.catalogo.resolver("CARTON")), "acopio-verde",
                "Ana Prueba", "099 111 222", "Calle 1", null, null, null, null));
        mostrar("ana", "centro de otra ciudad",
                app.postNueva(ana, Ciudad.RIVERA,
                        List.of(app.catalogo.resolver("CARTON")), "coleta-solidaria",
                        "Ana Prueba", "099 111 222", "Calle 1", null, null, null, null));
        mostrar("ana", "vidrio a acopio-verde",
                app.postNueva(ana, Ciudad.RIVERA,
                        List.of(app.catalogo.resolver("VIDRIO")), "acopio-verde",
                        "Ana Prueba", "099 111 222", "Calle 1", null, null, null, null));
        mostrar("ana", "mapa a medias (solo lat)",
                app.postNueva(ana, Ciudad.RIVERA,
                        List.of(app.catalogo.resolver("CARTON")), "acopio-verde",
                        "Ana Prueba", "099 111 222", "Calle 1", null, -30.9, null, null));
        mostrar("ana", "validar campo 'direccion' vacío",
                app.postNuevaValidar(ana, "direccion", ""));
        mostrar("ana", "validar campo 'nota' larga",
                app.postNuevaValidar(ana, "nota", "x".repeat(301)));

        titulo("Pedidos válidos: aviso NUEVA a la organización destino");
        mostrar("ana", "cartón+papel → frontera-limpia",
                app.postNueva(ana, Ciudad.RIVERA,
                        List.of(app.catalogo.resolver("CARTON"), app.catalogo.resolver("PAPEL")),
                        "frontera-limpia", "Ana Prueba", "099 111 222",
                        "Av. Italia 1234", "Portón rojo", -30.906, -55.552,
                        "Dejar en la vereda"));
        mostrar("ana", "plástico → acopio-verde",
                app.postNueva(ana, Ciudad.RIVERA,
                        List.of(app.catalogo.resolver("PLASTICO")), "acopio-verde",
                        "Ana Prueba", "099 111 222", "Av. Italia 1234", null,
                        null, null, null));
        mostrar("bruno", "vidrio → coleta-solidaria (Livramento)",
                app.postNueva(bruno, Ciudad.LIVRAMENTO,
                        List.of(app.catalogo.resolver("VIDRIO")), "coleta-solidaria",
                        "Bruno P", "099 555 000", "Rua Acre 55", null, null, null,
                        "Retirar de mañana"));

        titulo("La organización entra y ve su bandeja (ListadoService.recibidas)");
        mostrar("centro-norte", "POST entrar 'renacer-norte'",
                app.postEntrar(centroNorte, "renacer-norte", "centro-demo"));
        bloque(app.getOrgSolicitudes(centroNorte).cuerpo());
        mostrar("centro-jardins", "POST entrar 'novo-horizonte'",
                app.postEntrar(centroJardins, "novo-horizonte", "centro-demo"));
        bloque(app.getOrgSolicitudes(centroJardins).cuerpo());

        titulo("La org decide: aceptar, rechazar, completar (con @Version)");
        var pendienteAna = app.solicitudes.todas().stream()
                .filter(i -> i.nombreCiudadano().equals("Ana Prueba")
                        && i.estado() == Estado.PENDIENTE).findFirst().orElseThrow();
        long idAna = pendienteAna.id();
        long versionAna = pendienteAna.version();
        mostrar("centro-norte", "aceptar #" + idAna + " version=" + versionAna,
                app.postOrgAceptar(centroNorte, idAna, versionAna));
        mostrar("centro-norte", "aceptar #" + idAna + " de nuevo (versión vieja)",
                app.postOrgAceptar(centroNorte, idAna, versionAna));
        mostrar("centro-norte", "aceptar #" + idAna + " (versión actual pero EN_CURSO)",
                app.postOrgAceptar(centroNorte, idAna, versionAna + 1));
        var enCursoMartina = app.solicitudes.todas().stream()
                .filter(i -> i.nombreCiudadano().equals("Martina López")
                        && i.estado() == Estado.EN_CURSO).findFirst().orElseThrow();
        mostrar("centro-norte", "completar #" + enCursoMartina.id(),
                app.postOrgCompletar(centroNorte, enCursoMartina.id(),
                        enCursoMartina.version()));
        var pendienteAna2 = app.solicitudes.todas().stream()
                .filter(i -> i.nombreCiudadano().equals("Ana Rodríguez")
                        && i.estado() == Estado.PENDIENTE).findFirst().orElseThrow();
        mostrar("centro-norte", "rechazar #" + pendienteAna2.id(),
                app.postOrgRechazar(centroNorte, pendienteAna2.id(),
                        pendienteAna2.version()));
        nota("una org no puede tocar solicitudes de otra:");
        mostrar("centro-jardins", "aceptar #" + idAna + " de frontera-limpia",
                app.postOrgAceptar(centroJardins, idAna, 999));

        titulo("El ciudadano: su panel, detalle, cancelación");
        bloque(app.getMisSolicitudes(ana, null, 1).cuerpo());
        mostrar("ana", "GET detalle #" + idAna, app.getMisSolicitud(ana, idAna));
        mostrar("bruno", "GET detalle #" + idAna + " (ajena)",
                app.getMisSolicitud(bruno, idAna));
        var pendienteBruno = app.solicitudes.todas().stream()
                .filter(i -> i.nombreCiudadano().equals("Bruno Pérez")
                        && i.estado() == Estado.PENDIENTE).findFirst().orElseThrow();
        var canceladaAna = app.solicitudes.todas().stream()
                .filter(i -> i.nombreCiudadano().equals("Ana Prueba")
                        && i.organizacionNombre().contains("Microcentro"))
                .findFirst().orElseThrow();
        mostrar("ana", "cancelar #" + canceladaAna.id() + " (PENDIENTE propia)",
                app.postCancelar(ana, canceladaAna.id(), canceladaAna.version()));
        mostrar("ana", "cancelar #" + idAna + " (EN_CURSO → inválida)",
                app.postCancelar(ana, idAna,
                        app.solicitudes.todas().stream().filter(i -> i.id() == idAna)
                                .findFirst().orElseThrow().version()));
        mostrar("bruno", "cancelar #" + pendienteBruno.id() + " (de otro autor)",
                app.postCancelar(bruno, pendienteBruno.id(), pendienteBruno.version()));

        titulo("Avisos: bandeja, contador y lectura");
        mostrar("ana", "GET /avisos/contador (sin leer)",
                app.getAvisosContador(ana));
        bloque(app.getAvisos(ana).cuerpo());
        mostrar("ana", "contador tras abrir la bandeja", app.getAvisosContador(ana));
        bloque(app.getAvisos(centroNorte).cuerpo());
        bloque(app.getAvisos(centroJardins).cuerpo());

        titulo("Informes, archivadas y copia de respaldo");
        bloque(app.getOrgInforme(centroNorte, YearMonth.now().toString()).cuerpo());
        bloque(app.getMisInforme(ana, YearMonth.now().toString()).cuerpo());
        bloque(app.getOrgArchivadas(centroNorte, 1).cuerpo());
        bloque(app.getOrgCopia(centroNorte).cuerpo());
        mostrar("ana (ciudadana)", "GET /org/solicitudes/copia",
                app.getOrgCopia(ana));

        titulo("Perfil de la organización (validación + conflicto de versión)");
        var orgInfo = app.organizaciones.buscar("frontera-limpia").orElseThrow();
        mostrar("centro-norte", "teléfono corto",
                app.postOrgPerfil(centroNorte, orgInfo.materiales(),
                        "Lunes a sábado", "1234", null, null, orgInfo.version()));
        mostrar("centro-norte", "versión vieja (otra pestaña ganó)",
                app.postOrgPerfil(centroNorte, orgInfo.materiales(),
                        "Lunes a sábado, 7–19 h", "+598 92 000 111", null, null, 99));
        mostrar("centro-norte", "perfil válido (añade ELECTRONICOS)",
                app.postOrgPerfil(centroNorte,
                        List.of(app.catalogo.resolver("PLASTICO"),
                                app.catalogo.resolver("CARTON"), app.catalogo.resolver("PAPEL"),
                                app.catalogo.resolver("VIDRIO"), app.catalogo.resolver("METAL"),
                                app.catalogo.resolver("LATAS"),
                                app.catalogo.resolver("ELECTRONICOS")),
                        "Lunes a sábado, 7–19 h", "+598 92 000 111",
                        -30.9120, -55.5560, orgInfo.version()));
        nota("ahora frontera-limpia recibe ELECTRONICOS: "
                + app.organizaciones.buscar("frontera-limpia").orElseThrow().materiales()
                        .stream().map(Material::getCodigo).toList());

        titulo("Secciones públicas: acopios, comunidad, estadísticas");
        bloque(app.getAcopios(anonimo).cuerpo());
        bloque(app.getComunidad(null).cuerpo());
        bloque(app.getComunidad("historia").cuerpo());
        bloque(app.getEstadisticas("todo").cuerpo());
        bloque(app.getEstadisticas("30d").cuerpo());
        bloque(app.getEstadisticas("valor-malo → 30d").cuerpo());

        titulo("Opinión: respaldo del cuestionario por rol");
        var cuestionario = app.cuestionarios.get(Rol.CIUDADANO);
        var params = new HashMap<Integer, String>();
        for (int i = 0; i < cuestionario.preguntas().size(); i++) {
            var p = cuestionario.preguntas().get(i);
            switch (p.tipo()) {
                case OPCION -> params.put(i, p.opciones().get(0));
                case ESCALA -> params.put(i, "4");
                case TEXTO -> params.put(i, "Agregaría notificaciones por SMS.");
            }
        }
        mostrar("ana", "POST /opinion/respaldo completo",
                app.postOpinionRespaldo(ana, params));
        var malos = Map.of(0, "Respuesta inventada", 1, "9");
        mostrar("ana", "escala 9 / opción inventada",
                app.postOpinionRespaldo(ana, malos));
        mostrar("ana", "sin ninguna escala respondida",
                app.postOpinionRespaldo(ana, Map.of(15, "solo texto")));
        nota("respuestas registradas: " + app.respuestasOpinion.size());

        titulo("Salir y sesión limpia");
        mostrar("ana", "POST /cuenta/salir", app.postSalir(ana));
        mostrar("ana", "GET /mis-solicitudes tras salir",
                app.getMisSolicitudes(ana, null, 1));

        System.out.println("\n===== Fin de la simulación =====");
    }
}
