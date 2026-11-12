package com.ecosolicitud.cuenta.interno;

import java.io.IOException;

import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.shared.Seccion;
import com.ecosolicitud.shared.Volver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class IngresoHandler implements AuthenticationSuccessHandler {

    private final ObjectProvider<ActorSesion> actor;

    IngresoHandler(ObjectProvider<ActorSesion> actor) {
        this.actor = actor;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response, Authentication auth) throws IOException {
        String destino = Seccion.inicioDe(com.ecosolicitud.shared.Rol.CIUDADANO);
        if (auth.getPrincipal() instanceof UsuarioPrincipal u) {
            actor.getObject().autenticar(u.getCiudadanoId(), u.getNombre(),
                    u.getRol(), u.getOrganizacionId());
            destino = Seccion.inicioDe(u.getRol());
            // el vecino vuelve a la página que había pedido (QR, enlace compartido)
            if (u.getRol() == com.ecosolicitud.shared.Rol.CIUDADANO) {
                destino = Volver.tomar(request).orElse(destino);
            }
        } else if (auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            // la credencial del servidor no es una cuenta: va a su panel
            destino = Rutas.ADMIN;
        }
        response.sendRedirect(request.getContextPath() + destino);
    }
}
