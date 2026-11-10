package com.ecosolicitud.cuenta.interno;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ecosolicitud.cuenta.Cuentas;
import com.ecosolicitud.cuenta.Pais;
import com.ecosolicitud.cuenta.Telefono;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.shared.Seccion;
import com.ecosolicitud.shared.Volver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

    @GetMapping(Rutas.CUENTA_PERFIL)
    String perfil(Model modelo) {
        var sesion = actor.getObject();
        var actual = sesion.actual();
        if (actual.rol() != Rol.CIUDADANO) {
            return "redirect:" + Seccion.inicioDe(actual.rol());
        }
        var cuenta = usuarios.findById(actual.ciudadanoId());
        if (cuenta.isPresent()) {
            modelo.addAttribute("valores", Map.of(
                    "nombre", cuenta.get().getNombre(),
                    "telefono", Telefono.presentar(cuenta.get().getTelefono())));
            modelo.addAttribute("paisActual", paisDe(cuenta.get().getTelefono()).name());
        } else {
            // sesión demo: no hay fila de cuenta; se muestra la ficha vacía
            modelo.addAttribute("valores", Map.of(
                    "nombre", actual.nombreCiudadano(),
                    "telefono", ""));
            modelo.addAttribute("paisActual", Pais.UY.name());
        }
        modelo.addAttribute("errores", Map.of());
        return "secciones/cuenta/perfil";
    }

    @PostMapping(Rutas.CUENTA_PERFIL)
    String guardarPerfil(@RequestParam String nombre, @RequestParam String pais,
            @RequestParam String telefono, @RequestParam(required = false) String clave,
            Model modelo, RedirectAttributes redir) {
        var sesion = actor.getObject();
        var actual = sesion.actual();
        if (actual.rol() != Rol.CIUDADANO) {
            return "redirect:" + Seccion.inicioDe(actual.rol());
        }
        var cuenta = usuarios.findById(actual.ciudadanoId());
        if (cuenta.isEmpty()) {
            // sesión demo: sin cuenta real no hay nada que guardar
            return "redirect:" + Rutas.CUENTA_PERFIL;
        }
        Map<String, String> errores = new LinkedHashMap<>();
        Pais p;
        try {
            p = Pais.valueOf(pais == null ? "" : pais);
        } catch (IllegalArgumentException e) {
            p = null;
            errores.put("pais", "cuenta.error.pais");
        }
        if (p != null) {
            errores.putAll(cuentas.erroresDePerfil(
                    cuenta.get().getId(), nombre, telefono, p, clave));
        }
        if (errores.isEmpty()) {
            cuentas.actualizarPerfil(cuenta.get().getId(),
                    nombre.strip(), telefono, p, clave);
            sesion.cambiarNombreCiudadano(nombre.strip().toLowerCase());
            redir.addFlashAttribute("guardado", true);
            return "redirect:" + Rutas.CUENTA_PERFIL;
        }
        modelo.addAttribute("errores", errores);
        modelo.addAttribute("valores", Map.of(
                "nombre", nombre == null ? "" : nombre,
                "telefono", telefono == null ? "" : telefono));
        modelo.addAttribute("paisActual", pais == null ? "" : pais);
        return "secciones/cuenta/perfil";
    }

    // el país se infiere del celular guardado: 598 es Uruguay, 55 Brasil
    private static Pais paisDe(String telefono) {
        return telefono.startsWith("598") ? Pais.UY : Pais.BR;
    }

    @GetMapping(Rutas.CUENTA_ENTRAR)
    String entrar() {
        // sesión viva → no tiene sentido mostrar el ingreso de nuevo
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        return "secciones/cuenta/entrar";
    }

    @GetMapping(Rutas.CUENTA_REGISTRAR)
    String registrar(Model modelo) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        modelo.addAttribute("errores", Map.of());
        modelo.addAttribute("valores", Map.of());
        return "secciones/cuenta/registrar";
    }

    @PostMapping(Rutas.CUENTA_REGISTRAR)
    String crear(@RequestParam String nombre, @RequestParam String pais,
            @RequestParam String telefono, @RequestParam String clave,
            @RequestParam(required = false) String condiciones,
            HttpServletRequest request, RedirectAttributes redir, Model modelo) {
        // campo → clave de messages; la plantilla la traduce
        Map<String, String> errores = new LinkedHashMap<>();
        if (condiciones == null) {
            errores.put("condiciones", "cuenta.error.condiciones");
        }
        Pais p;
        try {
            p = Pais.valueOf(pais == null ? "" : pais);
        } catch (IllegalArgumentException e) {
            p = null;
            errores.put("pais", "cuenta.error.pais");
        }
        if (p != null) {
            errores.putAll(cuentas.errores(nombre, telefono, p, clave));
        }
        // reintento del mismo registro (red lenta): la cuenta ya existe y es
        // suya; entra en vez de mostrar "ese nombre ya está en uso"
        if (errores.containsKey("nombre") && errores.containsKey("telefono")
                && errores.values().stream().allMatch(e -> e.endsWith(".usado"))
                && cuentas.esLaMismaCuenta(nombre, telefono, p, clave)) {
            var principal = new UsuarioPrincipal(
                    usuarios.findByNombre(nombre.strip().toLowerCase()).orElseThrow());
            return entrarCon(principal, request, redir);
        }
        if (errores.isEmpty()) {
            var usuario = cuentas.registrarCiudadano(nombre.strip(), telefono, p, clave);
            return entrarCon(new UsuarioPrincipal(
                    usuarios.findByTelefono(usuario.telefono()).orElseThrow()), request, redir);
        }
        // lo escrito vuelve al formulario: corregir un campo no obliga a
        // reescribir los demás (la contraseña no se devuelve nunca)
        modelo.addAttribute("errores", errores);
        modelo.addAttribute("valores", Map.of(
                "nombre", nombre == null ? "" : nombre,
                "pais", pais == null ? "" : pais,
                "telefono", telefono == null ? "" : telefono,
                "condiciones", condiciones != null));
        return "secciones/cuenta/registrar";
    }

    private String entrarCon(UsuarioPrincipal principal, HttpServletRequest request,
            RedirectAttributes redir) {
        ingresar(request, principal);
        redir.addFlashAttribute("avisoCodigo", "cuenta.bienvenida");
        redir.addFlashAttribute("avisoId", principal.getNombre());
        return "redirect:" + Volver.tomar(request)
                .orElse(Seccion.inicioDe(principal.getRol()));
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
