package com.ecosolicitud;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.solicitud.AvisoService;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.CodigoQr;
import com.ecosolicitud.shared.Limites;
import com.ecosolicitud.shared.Miga;
import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.shared.Seccion;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.context.LazyContextVariable;

import static java.util.stream.Collectors.toUnmodifiableMap;

// Corre en todos los pedidos, también en los fragmentos htmx (el contador de
// avisos cada 60 s). Lo que consulta la base o arma el layout es perezoso:
// Thymeleaf lo calcula solo si la plantilla lo usa.
@ControllerAdvice
class NavegacionAdvice {

    // constantes de Rutas y Limites para las plantillas: ${rutas.NUEVA}, ${limites.NOTA}
    private static final Map<String, Object> RUTAS = constantes(Rutas.class);
    private static final Map<String, Object> LIMITES = constantes(Limites.class);
    // el QR solo depende de la raíz del despliegue: se genera una vez por raíz.
    // La raíz sale del Host del pedido, así que el caché tiene tope
    private static final int QR_MAXIMOS = 8;
    private final Map<String, String> qrs = new ConcurrentHashMap<>();

    private final ActorSesion actor;
    private final OrganizacionService organizaciones;
    private final AvisoService avisos;
    private final boolean demoHabilitada;

    NavegacionAdvice(ActorSesion actor, OrganizacionService organizaciones,
            AvisoService avisos,
            @Value("${ecosolicitud.demo.habilitada:true}") boolean demoHabilitada) {
        this.actor = actor;
        this.organizaciones = organizaciones;
        this.avisos = avisos;
        this.demoHabilitada = demoHabilitada;
    }

    @ModelAttribute("rutaActual")
    String rutaActual(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("rutas")
    Map<String, Object> rutas() {
        return RUTAS;
    }

    @ModelAttribute("limites")
    Map<String, Object> limites() {
        return LIMITES;
    }

    @ModelAttribute("rolActual")
    Rol rolActual() {
        return actor.get();
    }

    @ModelAttribute("secciones")
    List<Seccion> secciones(HttpServletRequest request) {
        return Seccion.principales(actor.get(), autenticado(request));
    }

    @ModelAttribute("seccionesMas")
    List<Seccion> seccionesMas(HttpServletRequest request) {
        return Seccion.mas(actor.get(), autenticado(request));
    }

    // "Más" llega abierto si la página actual vive adentro

    // cuenta real o sesión demo-admin: ambas ya pasaron por un login
    private static boolean autenticado(HttpServletRequest r) {
        return r.isUserInRole("CIUDADANO") || r.isUserInRole("ORGANIZACION")
                || r.isUserInRole("DEMO");
    }

    @ModelAttribute("organizaciones")
    LazyContextVariable<List<OrganizacionInfo>> organizaciones() {
        return perezosa(() -> actor.get() == Rol.ORGANIZACION ? organizaciones.todas() : List.of());
    }

    @ModelAttribute("organizacionActiva")
    LazyContextVariable<OrganizacionInfo> organizacionActiva() {
        return perezosa(() -> actor.get() == Rol.ORGANIZACION
                ? organizaciones.actual(actor.actual()) : null);
    }

    // dos niveles como máximo: Inicio › Sección, o Sección › Página en las
    // subrutas (la sección queda enlazada y el controller agrega la miga
    // final, atributo "migaFinal"). La miga reemplaza a los enlaces "Volver".
    @ModelAttribute("migas")
    LazyContextVariable<List<Miga>> migas(HttpServletRequest request) {
        var ruta = request.getRequestURI();
        return perezosa(() -> migasDe(ruta));
    }

    private static List<Miga> migasDe(String ruta) {
        try {
            var s = Seccion.desdeRuta(ruta);
            return s.getRuta().equals(ruta)
                    ? List.of(Miga.inicio(), Miga.actualClave(s.getClave()))
                    : List.of(Miga.seccion(s));
        } catch (ResponseStatusException e) {
            var clave = Map.of(
                    Rutas.CUENTA_ENTRAR, "cuenta.entrar.titulo",
                    Rutas.CUENTA_REGISTRAR, "cuenta.registrar.titulo",
                    Rutas.CUENTA_CONDICIONES, "cuenta.condiciones.titulo",
                    Rutas.MODO_DEV, "modo.titulo").get(ruta);
            return clave != null
                    ? List.of(Miga.inicio(), Miga.actualClave(clave))
                    : List.of(ruta.equals("/") ? Miga.actualClave("miga.inicio")
                            : Miga.inicio());
        }
    }

    @ModelAttribute("avisosSinLeer")
    LazyContextVariable<Long> avisosSinLeer() {
        return perezosa(() -> avisos.avisosSinLeer(actor.actual()));
    }

    // el QR del pie del menú apunta a la raíz del despliegue
    @ModelAttribute("qr")
    LazyContextVariable<String> qr() {
        return perezosa(this::qrDeLaRaiz);
    }

    private String qrDeLaRaiz() {
        var raiz = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        var guardado = qrs.get(raiz);
        if (guardado != null) {
            return guardado;
        }
        var svg = CodigoQr.svg(raiz);
        if (qrs.size() < QR_MAXIMOS) {
            qrs.putIfAbsent(raiz, svg);
        }
        return svg;
    }

    @ModelAttribute("demoHabilitada")
    boolean demoHabilitada(HttpServletRequest request) {
        return demoHabilitada && request.isUserInRole("DEMO");
    }

    // nombre de la cuenta real autenticada (la demo usa ROLE_DEMO, no cuenta)
    @ModelAttribute("cuentaNombre")
    String cuentaNombre(HttpServletRequest request) {
        return request.isUserInRole("CIUDADANO") || request.isUserInRole("ORGANIZACION")
                ? actor.actual().nombreCiudadano() : null;
    }

    private static <T> LazyContextVariable<T> perezosa(Supplier<T> valor) {
        return new LazyContextVariable<>() {
            @Override
            protected T loadValue() {
                return valor.get();
            }
        };
    }

    private static Map<String, Object> constantes(Class<?> clase) {
        return Arrays.stream(clase.getFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .collect(toUnmodifiableMap(Field::getName, f -> {
                    try {
                        return f.get(null);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }));
    }
}
