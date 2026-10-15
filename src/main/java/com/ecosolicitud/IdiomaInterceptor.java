package com.ecosolicitud;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.ecosolicitud.shared.Rutas;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;

// La ciudad implica idioma: el selector de la portada fija PT para
// Livramento y ES para las demás, salvo elección manual con ES/PT.
class IdiomaInterceptor implements HandlerInterceptor {

    private static final String MANUAL = "idiomaManual";
    private final LocaleResolver locales;

    IdiomaInterceptor(LocaleResolver locales) {
        this.locales = locales;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getParameter("lang") != null) {
            request.getSession().setAttribute(MANUAL, true);
            return true;
        }
        var ciudad = request.getParameter("ciudad");
        if (ciudad == null || !esSelectorDeCiudad(request)) {
            return true;
        }
        var sesion = request.getSession(false);
        if (sesion == null || sesion.getAttribute(MANUAL) == null) {
            locales.setLocale(request, response, Localidad.desde(ciudad).getIdioma());
        }
        return true;
    }

    private boolean esSelectorDeCiudad(HttpServletRequest request) {
        var ruta = request.getServletPath();
        return Rutas.RAIZ.equals(ruta);
    }
}
