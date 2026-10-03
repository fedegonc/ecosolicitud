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

    public Actor actual() {
        return new Actor(rol, ciudadanoId, nombreCiudadano, organizacionId);
    }

    public void cambiar(Rol rol) {
        this.rol = rol;
    }

    public void elegirOrganizacion(String organizacionId) {
        this.organizacionId = organizacionId;
    }
}
