package com.ecosolicitud.shared;

import java.net.URI;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

// La página privada que pidió un vecino sin sesión (p. ej. un QR a /nueva):
// se guarda al mandarlo a Ingresar y se retoma al entrar o registrarse.
// Solo rutas internas de navegación: nunca otra web ni un fragmento htmx.
public final class Volver {

    private static final String CLAVE = "ecosolicitud.volver";

    public static void guardar(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod()) || esFragmento(request)) {
            return;
        }
        var ruta = request.getRequestURI().substring(request.getContextPath().length());
        if (request.getQueryString() != null) {
            ruta += "?" + request.getQueryString();
        }
        if (interna(ruta)) {
            request.getSession().setAttribute(CLAVE, ruta);
        }
    }

    // un fragmento htmx pedido sin sesión: se recuerda la página desde donde
    // se pidió (HX-Current-URL). Solo su ruta, nunca el host que traiga.
    public static void guardarPagina(HttpServletRequest request) {
        var actual = request.getHeader("HX-Current-URL");
        if (actual == null) {
            return;
        }
        try {
            var uri = URI.create(actual);
            var ruta = uri.getRawPath() == null ? "" : uri.getRawPath();
            if (ruta.startsWith(request.getContextPath())) {
                ruta = ruta.substring(request.getContextPath().length());
            }
            if (uri.getRawQuery() != null) {
                ruta += "?" + uri.getRawQuery();
            }
            if (interna(ruta)) {
                request.getSession().setAttribute(CLAVE, ruta);
            }
        } catch (IllegalArgumentException e) {
            // cabecera inválida: no se recuerda nada
        }
    }

    // la ruta guardada, una sola vez
    public static Optional<String> tomar(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null || !(sesion.getAttribute(CLAVE) instanceof String ruta)) {
            return Optional.empty();
        }
        sesion.removeAttribute(CLAVE);
        return interna(ruta) ? Optional.of(ruta) : Optional.empty();
    }

    // un pedido htmx que no es navegación (boost) trae un pedazo de página
    public static boolean esFragmento(HttpServletRequest request) {
        return request.getHeader("HX-Request") != null
                && request.getHeader("HX-Boosted") == null;
    }

    static boolean interna(String ruta) {
        return ruta.startsWith("/") && !ruta.startsWith("//") && !ruta.contains("\\")
                && !ruta.startsWith(Rutas.AVISOS_CONTADOR) && !ruta.startsWith(Rutas.NUEVA_VALIDAR);
    }

    private Volver() {
    }
}
