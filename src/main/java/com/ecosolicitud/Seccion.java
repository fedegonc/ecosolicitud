package com.ecosolicitud;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum Seccion {
    INICIO("seccion.inicio", "/inicio", Rol.CIUDADANO, "casa"),
    NUEVA("seccion.nueva", "/nueva", Rol.CIUDADANO, "mas"),
    MIS_SOLICITUDES("seccion.mis-solicitudes", "/mis-solicitudes", Rol.CIUDADANO, "lista"),
    ACOPIOS("seccion.acopios", "/acopios", Rol.CIUDADANO, "edificio"),
    NOTIFICACIONES("seccion.notificaciones", "/notificaciones", Rol.CIUDADANO, "campana"),
    PERFIL("seccion.perfil", "/perfil", Rol.CIUDADANO, "usuario"),
    ORG_PANEL("seccion.org.panel", "/org/panel", Rol.ORGANIZACION, "panel"),
    ORG_SOLICITUDES("seccion.org.solicitudes", "/org/solicitudes", Rol.ORGANIZACION, "bandeja"),
    ORG_MATERIALES("seccion.org.materiales", "/org/materiales", Rol.ORGANIZACION, "cajas"),
    ORG_REPORTES("seccion.org.reportes", "/org/reportes", Rol.ORGANIZACION, "grafico");

    private final String clave;
    private final String ruta;
    private final Rol rol;
    private final String icono;

    Seccion(String clave, String ruta, Rol rol, String icono) {
        this.clave = clave;
        this.ruta = ruta;
        this.rol = rol;
        this.icono = icono;
    }

    public String getClave() {
        return clave;
    }

    public String getRuta() {
        return ruta;
    }

    public Rol getRol() {
        return rol;
    }

    public String getIcono() {
        return icono;
    }

    public static List<Seccion> paraRol(Rol rol) {
        return Arrays.stream(values()).filter(s -> s.rol == rol).toList();
    }

    public static Seccion desdeRuta(String ruta) {
        return Arrays.stream(values())
                .filter(s -> s.ruta.equals(ruta))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
