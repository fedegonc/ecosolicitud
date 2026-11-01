package com.ecosolicitud;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

import com.ecosolicitud.shared.Rutas;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;

// El formulario de /nueva quedó abierto y la sesión venció o el servidor se
// reinició: el token CSRF ya no vale. En vez de "Algo salió mal" y perder lo
// escrito, vuelve a /nueva con los mismos datos (el GET los rellena) para
// tocar Enviar otra vez. Sin sesión, pasa por Ingresar y vuelve (Volver).
final class FormularioVencido implements AccessDeniedHandler {

    static final String MARCA = "vencida";
    private static final int MAXIMO = 4000;

    private final AccessDeniedHandler resto = new AccessDeniedHandlerImpl();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException error) throws IOException, ServletException {
        var ruta = request.getRequestURI().substring(request.getContextPath().length());
        if (!(error instanceof CsrfException) || !"POST".equals(request.getMethod())
                || !Rutas.NUEVA.equals(ruta) || request.getHeader("HX-Request") != null) {
            resto.handle(request, response, error);
            return;
        }
        response.sendRedirect(request.getContextPath() + destino(request));
    }

    static String destino(HttpServletRequest request) {
        var query = new StringJoiner("&");
        request.getParameterMap().forEach((nombre, valores) -> {
            if (!nombre.startsWith("_")) {
                for (var v : valores) {
                    query.add(cod(nombre) + "=" + cod(v));
                }
            }
        });
        query.add(MARCA);
        var url = Rutas.NUEVA + "?" + query;
        return url.length() <= MAXIMO ? url : Rutas.NUEVA + "?" + MARCA;
    }

    private static String cod(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
