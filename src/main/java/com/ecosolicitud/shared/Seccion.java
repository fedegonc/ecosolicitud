package com.ecosolicitud.shared;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum Seccion {
    ORG_SOLICITUDES("seccion.org.solicitudes", Rutas.ORG_SOLICITUDES, "bandeja", Rol.ORGANIZACION),
    ORG_PERFIL("seccion.org.perfil", Rutas.ORG_PERFIL, "edificio", Rol.ORGANIZACION),
    NUEVA("seccion.nueva", Rutas.NUEVA, "mas", Rol.CIUDADANO),
    MIS_SOLICITUDES("seccion.mis-solicitudes", Rutas.MIS_SOLICITUDES, "lista", Rol.CIUDADANO),
    AVISOS("seccion.avisos", Rutas.AVISOS, "campana", Rol.CIUDADANO, Rol.ORGANIZACION),
    ACOPIOS("seccion.acopios", Rutas.ACOPIOS, "edificio", Rol.CIUDADANO),
    GUIA("seccion.guia", Rutas.GUIA, "libro", Rol.CIUDADANO, Rol.ORGANIZACION),
    COMUNIDAD("seccion.comunidad", Rutas.COMUNIDAD, "gente", Rol.CIUDADANO,
            Rol.ORGANIZACION),
    ESTADISTICAS("seccion.estadisticas", Rutas.ESTADISTICAS, "grafico",
            Rol.CIUDADANO, Rol.ORGANIZACION),
    AYUDA("seccion.ayuda", Rutas.AYUDA, "ayuda", Rol.CIUDADANO, Rol.ORGANIZACION),
    OPINION("opinion.enlace", Rutas.OPINION, "mensaje", Rol.CIUDADANO,
            Rol.ORGANIZACION);

    private final String clave;
    private final String ruta;
    private final String icono;
    private final Set<Rol> roles;

    Seccion(String clave, String ruta, String icono, Rol... roles) {
        this.clave = clave;
        this.ruta = ruta;
        this.icono = icono;
        this.roles = EnumSet.copyOf(List.of(roles));
    }

    public String getClave() {
        return clave;
    }

    public String getRuta() {
        return ruta;
    }

    public String getIcono() {
        return icono;
    }

    public boolean permite(Rol rol) {
        return roles.contains(rol);
    }

    // datos personales o acciones de cuenta: solo con sesión activa
    private static final Set<Seccion> PRIVADAS = EnumSet.of(
            ORG_SOLICITUDES, ORG_PERFIL, NUEVA, MIS_SOLICITUDES, AVISOS);

    public boolean publica() {
        return !PRIVADAS.contains(this);
    }

    public static List<Seccion> principales(Rol rol, boolean autenticado) {
        var r = principales(rol);
        return autenticado ? r : r.stream().filter(Seccion::publica).toList();
    }

    public static List<Seccion> mas(Rol rol, boolean autenticado) {
        var r = mas(rol);
        return autenticado ? r : r.stream().filter(Seccion::publica).toList();
    }

    public static List<Seccion> paraRol(Rol rol) {
        return Arrays.stream(values()).filter(s -> s.permite(rol)).toList();
    }

    // las cuatro del menú; el resto va dentro de "Más"
    public static List<Seccion> principales(Rol rol) {
        var cuatro = rol == Rol.ORGANIZACION
                ? EnumSet.of(ORG_SOLICITUDES, ORG_PERFIL, AVISOS, GUIA)
                : EnumSet.of(NUEVA, MIS_SOLICITUDES, AVISOS, ACOPIOS);
        return paraRol(rol).stream().filter(cuatro::contains).toList();
    }

    // la opinión no entra al menú: tiene su propio pie en cada página
    public static List<Seccion> mas(Rol rol) {
        var principales = principales(rol);
        return paraRol(rol).stream()
                .filter(s -> s != OPINION && !principales.contains(s)).toList();
    }

    public static String inicioDe(Rol rol) {
        return rol == Rol.ORGANIZACION ? Rutas.ORG_SOLICITUDES : Rutas.MIS_SOLICITUDES;
    }

    public static Seccion desdeRuta(String ruta) {
        return Arrays.stream(values())
                .filter(s -> s.ruta.equals(ruta) || ruta.startsWith(s.ruta + "/"))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public static String[] rutas() {
        return Arrays.stream(values()).map(s -> s.ruta).toArray(String[]::new);
    }

    public static boolean existeRuta(String ruta) {
        return Arrays.stream(values()).anyMatch(s -> s.ruta.equals(ruta));
    }

    public static String rutaSegura(String volver, Rol rol) {
        return volver != null && existeRuta(volver) ? volver : inicioDe(rol);
    }
}
