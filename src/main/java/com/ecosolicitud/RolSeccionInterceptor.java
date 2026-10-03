package com.ecosolicitud;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;

class RolSeccionInterceptor implements HandlerInterceptor {

    private final RolSesion rolSesion;

    RolSeccionInterceptor(RolSesion rolSesion) {
        this.rolSesion = rolSesion;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Rol rol = rolSesion.get();
        if (Seccion.desdeRuta(request.getRequestURI()).permite(rol)) {
            return true;
        }
        response.sendRedirect(Seccion.inicioDe(rol).getRuta());
        return false;
    }
}
