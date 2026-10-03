package com.ecosolicitud;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component
@SessionScope
public class RolSesion {

    private Rol rol = Rol.CIUDADANO;

    public Rol get() {
        return rol;
    }

    public void cambiar(Rol rol) {
        this.rol = rol;
    }
}
