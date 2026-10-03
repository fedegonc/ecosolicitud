package com.ecosolicitud;

import java.io.IOException;

import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Rol;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;

class RolSeccionInterceptor implements HandlerInterceptor {

    private final ActorSesion actor;

    RolSeccionInterceptor(ActorSesion actor) {
        this.actor = actor;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Rol rol = actor.get();
        if (Seccion.desdeRuta(request.getRequestURI()).permite(rol)) {
            return true;
        }
        response.sendRedirect(Seccion.inicioDe(rol));
        return false;
    }
}
