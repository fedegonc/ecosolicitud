package com.ecosolicitud;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum Seccion {
    ORG_PANEL("seccion.org.panel", "/org/panel", "panel", Rol.ORGANIZACION),
    ORG_SOLICITUDES("seccion.org.solicitudes", "/org/solicitudes", "bandeja", Rol.ORGANIZACION),
    ORG_REPORTE("seccion.org.reporte", "/org/reporte", "grafico", Rol.ORGANIZACION),
    ORG_PERFIL("seccion.org.perfil", "/org/perfil", "edificio", Rol.ORGANIZACION),
    INICIO("seccion.inicio", "/inicio", "casa", Rol.CIUDADANO),
    NUEVA("seccion.nueva", "/nueva", "mas", Rol.CIUDADANO),
    MIS_SOLICITUDES("seccion.mis-solicitudes", "/mis-solicitudes", "lista", Rol.CIUDADANO),
    ACOPIOS("seccion.acopios", "/acopios", "edificio", Rol.CIUDADANO),
    NOTIFICACIONES("seccion.notificaciones", "/notificaciones", "campana",
            Rol.CIUDADANO, Rol.ORGANIZACION),
    PERFIL("seccion.perfil", "/perfil", "usuario", Rol.CIUDADANO);

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

    public static Seccion inicioDe(Rol rol) {
        return paraRol(rol).getFirst();
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
}
