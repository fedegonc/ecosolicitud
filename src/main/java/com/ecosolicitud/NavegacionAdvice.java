package com.ecosolicitud;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

import static java.util.stream.Collectors.toUnmodifiableMap;

@ControllerAdvice
class NavegacionAdvice {

    // constantes de Rutas y Limites para las plantillas: ${rutas.NUEVA}, ${limites.NOTA}
    private static final Map<String, Object> RUTAS = constantes(Rutas.class);
    private static final Map<String, Object> LIMITES = constantes(Limites.class);

    private final ActorSesion actor;
    private final OrganizacionService organizaciones;
    private final AvisoService avisos;
    private final boolean demoHabilitada;
    private final boolean demoReinicio;

    NavegacionAdvice(ActorSesion actor, OrganizacionService organizaciones,
            AvisoService avisos,
            @Value("${ecosolicitud.demo.habilitada:true}") boolean demoHabilitada,
            @Value("${ecosolicitud.demo.reiniciar:true}") boolean demoReinicio) {
        this.actor = actor;
        this.organizaciones = organizaciones;
        this.avisos = avisos;
        this.demoHabilitada = demoHabilitada;
        this.demoReinicio = demoReinicio;
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
    List<Seccion> secciones() {
        return Seccion.principales(actor.get());
    }

    @ModelAttribute("seccionesMas")
    List<Seccion> seccionesMas() {
        return Seccion.mas(actor.get());
    }

    // "Más" llega abierto si la página actual vive adentro
    @ModelAttribute("masAbierto")
    boolean masAbierto(HttpServletRequest request) {
        var ruta = request.getRequestURI();
        return Seccion.mas(actor.get()).stream()
                .anyMatch(s -> ruta.equals(s.getRuta()) || ruta.startsWith(s.getRuta() + "/"));
    }

    @ModelAttribute("organizaciones")
    List<OrganizacionInfo> organizaciones() {
        return actor.get() == Rol.ORGANIZACION ? organizaciones.todas() : List.of();
    }

    @ModelAttribute("organizacionActiva")
    OrganizacionInfo organizacionActiva() {
        return actor.get() == Rol.ORGANIZACION ? organizaciones.actual(actor.actual()) : null;
    }

    // dos niveles como máximo: Inicio › Sección, o Sección › Página en las
    // subrutas (la sección queda enlazada y el controller agrega la miga
    // final, atributo "migaFinal"). La miga reemplaza a los enlaces "Volver".
    @ModelAttribute("migas")
    List<Miga> migas(HttpServletRequest request) {
        var ruta = request.getRequestURI();
        try {
            var s = Seccion.desdeRuta(ruta);
            return s.getRuta().equals(ruta)
                    ? List.of(Miga.inicio(), Miga.actualClave(s.getClave()))
                    : List.of(Miga.seccion(s));
        } catch (ResponseStatusException e) {
            return List.of(ruta.equals("/") ? Miga.actualClave("miga.inicio")
                    : Miga.inicio());
        }
    }

    @ModelAttribute("avisosSinLeer")
    long avisosSinLeer() {
        return avisos.avisosSinLeer(actor.actual());
    }

    // el QR del pie del menú apunta a la raíz del despliegue
    @ModelAttribute("qr")
    String qr() {
        return CodigoQr.svg(
                ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString());
    }

    @ModelAttribute("demoHabilitada")
    boolean demoHabilitada() {
        return demoHabilitada;
    }

    @ModelAttribute("demoReinicio")
    boolean demoReinicio() {
        return demoReinicio;
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
