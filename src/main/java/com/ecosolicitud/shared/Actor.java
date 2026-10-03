package com.ecosolicitud.shared;

public record Actor(Rol rol, String ciudadanoId, String nombreCiudadano, String organizacionId) {

    public boolean esOrganizacion() {
        return rol == Rol.ORGANIZACION;
    }

    public boolean esCiudadano() {
        return rol == Rol.CIUDADANO;
    }
}
