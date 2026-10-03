package com.ecosolicitud.shared;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component
@SessionScope
public class ActorSesion {

    private Rol rol = Rol.CIUDADANO;
    private String organizacionId;

    public Rol get() {
        return rol;
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
