package com.ecosolicitud.solicitud;

import com.ecosolicitud.shared.Actor;

// Quién puede ver una solicitud: el ciudadano que la creó y la organización
// a la que va dirigida. Se chequea en el servidor aunque el id venga en la URL.
public final class Propiedad {

    private Propiedad() {
    }

    public static boolean esAutor(Actor actor, String ciudadanoId) {
        return actor.esCiudadano() && ciudadanoId.equals(actor.ciudadanoId());
    }

    public static boolean esDestinataria(Actor actor, String organizacionId) {
        return actor.esOrganizacion() && organizacionId.equals(actor.organizacionId());
    }

    public static boolean puedeVer(Actor actor, String ciudadanoId, String organizacionId) {
        return esAutor(actor, ciudadanoId) || esDestinataria(actor, organizacionId);
    }
}
