package com.ecosolicitud.shared;

// Miga de pan: clave = mensaje i18n resoluble por el template,
// literal = texto ya resuelto (títulos de contenido), url = destino
// (null cuando la miga es la página actual, que no se enlaza).
public record Miga(String clave, String literal, String url) {

    public static Miga inicio() {
        return new Miga("miga.inicio", null, "/");
    }

    public static Miga seccion(Seccion s) {
        return new Miga(s.getClave(), null, s.getRuta());
    }

    public static Miga actualClave(String clave) {
        return new Miga(clave, null, null);
    }

    public static Miga actual(String literal) {
        return new Miga(null, literal, null);
    }
}
