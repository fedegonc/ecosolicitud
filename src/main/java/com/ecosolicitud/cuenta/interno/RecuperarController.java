package com.ecosolicitud.cuenta.interno;

import java.util.Map;

import com.ecosolicitud.cuenta.Cuentas;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Rutas;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Recuperar clave en dos pasos: el email pide el código y el código (que
// viajó por correo) abre la sesión. Respuesta genérica en ambos pasos:
// nunca se confirma si el email está registrado ni si el código era de él.
@Controller
class RecuperarController {

    private final RecuperacionService recuperacion;
    private final ObjectProvider<ActorSesion> actor;

    RecuperarController(RecuperacionService recuperacion, ObjectProvider<ActorSesion> actor) {
        this.recuperacion = recuperacion;
        this.actor = actor;
    }

    @GetMapping(Rutas.CUENTA_RECUPERAR)
    String form(Model modelo) {
        if (autenticado()) {
            return "redirect:" + Rutas.CUENTA_PERFIL;
        }
        modelo.addAttribute("errores", Map.of());
        modelo.addAttribute("valores", Map.of("email", ""));
        return "secciones/cuenta/recuperar";
    }

    @PostMapping(Rutas.CUENTA_RECUPERAR)
    String enviar(@RequestParam String email, Model modelo, RedirectAttributes redir) {
        var normalizado = Cuentas.normalizarEmail(email);
        if (normalizado == null) {
            modelo.addAttribute("errores", Map.of("email", "cuenta.error.email"));
            modelo.addAttribute("valores",
                    Map.of("email", email == null ? "" : email));
            return "secciones/cuenta/recuperar";
        }
        // sin correo configurado (dev) el código vuelve para el banner;
        // en producción siempre es empty: nunca viaja al HTML
        recuperacion.pedirCodigo(normalizado)
                .ifPresent(codigo -> redir.addFlashAttribute("codigoDev", codigo));
        redir.addFlashAttribute("email", normalizado);
        redir.addFlashAttribute("enviado", true);
        return "redirect:" + Rutas.CUENTA_RECUPERAR;
    }

    @PostMapping(Rutas.CUENTA_RECUPERAR_VERIFICAR)
    String verificar(@RequestParam String email, @RequestParam String codigo,
            HttpServletRequest request, Model modelo, RedirectAttributes redir) {
        var cuenta = recuperacion.verificar(Cuentas.normalizarEmail(email), codigo);
        if (cuenta.isEmpty()) {
            modelo.addAttribute("email", email);
            modelo.addAttribute("enviado", true);
            modelo.addAttribute("errores",
                    Map.of("codigo", "cuenta.recuperar.codigo.error"));
            return "secciones/cuenta/recuperar";
        }
        IngresoSesion.entrar(request, new UsuarioPrincipal(cuenta.get()), actor.getObject());
        redir.addFlashAttribute("recuperada", true);
        return "redirect:" + Rutas.CUENTA_PERFIL;
    }

    private static boolean autenticado() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }
}
