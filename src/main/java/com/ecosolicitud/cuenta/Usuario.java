package com.ecosolicitud.cuenta;

import com.ecosolicitud.shared.Rol;

// organizacionId solo para Rol.ORGANIZACION; el id del ciudadano es su ciudadanoId
public record Usuario(String id, String telefono, String claveHash, Rol rol, String organizacionId) {
}
