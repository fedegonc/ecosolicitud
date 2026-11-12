package com.ecosolicitud.cuenta.interno;

import com.ecosolicitud.shared.ActorSesion;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

// el mismo "entrar" del login y del registro: autentica el contexto, rota
// el id de sesión contra fijación y despierta al actor de la app
final class IngresoSesion {

    private IngresoSesion() {
    }

    static void entrar(HttpServletRequest request, UsuarioPrincipal principal,
            ActorSesion actor) {
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(contexto);
        request.changeSessionId();
        request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, contexto);
        actor.autenticar(principal.getCiudadanoId(), principal.getNombre(),
                principal.getRol(), principal.getOrganizacionId());
    }
}
