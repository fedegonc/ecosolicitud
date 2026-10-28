package com.ecosolicitud.cuenta.interno;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ecosolicitud.cuenta.CuentaException;
import com.ecosolicitud.cuenta.Cuentas;
import com.ecosolicitud.cuenta.Pais;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.shared.Seccion;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class CuentaController {

    private final Cuentas cuentas;
    private final UsuariosRepository usuarios;
    private final ObjectProvider<ActorSesion> actor;

    CuentaController(Cuentas cuentas, UsuariosRepository usuarios,
            ObjectProvider<ActorSesion> actor) {
        this.cuentas = cuentas;
        this.usuarios = usuarios;
        this.actor = actor;
    }

    @GetMapping(Rutas.CUENTA_CONDICIONES)
    String condiciones() {
        return "secciones/cuenta/condiciones";
    }

    @GetMapping(Rutas.CUENTA_ENTRAR)
    String entrar() {
        return "secciones/cuenta/entrar";
    }

    @GetMapping(Rutas.CUENTA_REGISTRAR)
    String registrar(Model modelo) {
        modelo.addAttribute("errores", Map.of());
        return "secciones/cuenta/registrar";
    }

    @PostMapping(Rutas.CUENTA_REGISTRAR)
    String crear(@RequestParam String nombre, @RequestParam String pais,
            @RequestParam String telefono, @RequestParam String clave,
            @RequestParam(required = false) String condiciones,
            HttpServletRequest request, RedirectAttributes redir, Model modelo) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (condiciones == null) {
            errores.put("condiciones", "Tenés que aceptar las condiciones de uso");
        }
        Pais p;
        try {
            p = Pais.valueOf(pais == null ? "" : pais);
        } catch (IllegalArgumentException e) {
            p = null;
            errores.put("pais", "Elegí Uruguay o Brasil");
        }
        if (p != null) {
            try {
                cuentas.validarDatos(nombre, telefono, p, clave);
            } catch (CuentaException e) {
                errores.put(e.campo(), e.getMessage());
            }
        }
        if (errores.isEmpty()) {
            var usuario = cuentas.registrarCiudadano(nombre.strip(), telefono, p, clave);
            var principal = new UsuarioPrincipal(
                    usuarios.findByTelefono(usuario.telefono()).orElseThrow());
            ingresar(request, principal);
            redir.addFlashAttribute("avisoCodigo", "cuenta.bienvenida");
            redir.addFlashAttribute("avisoId", principal.getNombre());
            return "redirect:" + Seccion.inicioDe(principal.getRol());
        }
        modelo.addAttribute("errores", errores);
        return "secciones/cuenta/registrar";
    }

    // registro = entrada inmediata: sesión autenticada y el mismo actor que
    // el handler de login; la rotación del id evita fijación de sesión
    private void ingresar(HttpServletRequest request, UsuarioPrincipal principal) {
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(contexto);
        request.changeSessionId();
        request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, contexto);
        actor.getObject().autenticar(principal.getCiudadanoId(), principal.getNombre(),
                principal.getRol(), principal.getOrganizacionId());
    }
}
