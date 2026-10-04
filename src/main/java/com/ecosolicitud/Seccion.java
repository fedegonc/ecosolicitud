package com.ecosolicitud;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.shared.Rol;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum Seccion {
    ORG_SOLICITUDES("seccion.org.solicitudes", Rutas.ORG_SOLICITUDES, "bandeja", Rol.ORGANIZACION),
    ORG_PERFIL("seccion.org.perfil", Rutas.ORG_PERFIL, "edificio", Rol.ORGANIZACION),
    NUEVA("seccion.nueva", Rutas.NUEVA, "mas", Rol.CIUDADANO),
    MIS_SOLICITUDES("seccion.mis-solicitudes", Rutas.MIS_SOLICITUDES, "lista", Rol.CIUDADANO),
    ACOPIOS("seccion.acopios", Rutas.ACOPIOS, "edificio", Rol.CIUDADANO),
    GUIA("seccion.guia", Rutas.GUIA, "libro", Rol.CIUDADANO, Rol.ORGANIZACION),
    COMUNIDAD("seccion.comunidad", Rutas.COMUNIDAD, "gente", Rol.CIUDADANO,
            Rol.ORGANIZACION),
    ESTADISTICAS("seccion.estadisticas", Rutas.ESTADISTICAS, "grafico",
            Rol.CIUDADANO, Rol.ORGANIZACION);

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

    public static List<Seccion> paraRol(Rol rol) {
        return Arrays.stream(values()).filter(s -> s.permite(rol)).toList();
    }

    public static String inicioDe(Rol rol) {
        return rol == Rol.ORGANIZACION ? Rutas.ORG_SOLICITUDES : Rutas.MIS_SOLICITUDES;
    }

    public static Seccion desdeRuta(String ruta) {
        return Arrays.stream(values())
                .filter(s -> s.ruta.equals(ruta))
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
