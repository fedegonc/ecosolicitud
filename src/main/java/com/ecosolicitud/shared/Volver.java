package com.ecosolicitud.shared;

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
    private static boolean esFragmento(HttpServletRequest request) {
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
