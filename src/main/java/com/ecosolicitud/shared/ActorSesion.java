package com.ecosolicitud.shared;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component
@SessionScope
public class ActorSesion {

    private Rol rol = Rol.CIUDADANO;
    private String organizacionId;
    private String ciudadanoId = "ciudadano-demo";
    private String nombreCiudadano = "Ciudadano demo";

    public Rol get() {
        return rol;
    }

    public String getCiudadanoId() {
        return ciudadanoId;
    }

    public String getNombreCiudadano() {
        return nombreCiudadano;
    }

    public void cambiar(Rol rol) {
        this.rol = rol;
    }

    public String getOrganizacionId() {
        return organizacionId;
    }

    public void elegirOrganizacion(String organizacionId) {
        this.organizacionId = organizacionId;
    }
}
