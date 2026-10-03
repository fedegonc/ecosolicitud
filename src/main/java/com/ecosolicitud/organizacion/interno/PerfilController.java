package com.ecosolicitud.organizacion.interno;

import com.ecosolicitud.Rutas;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Material;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class PerfilController {

    private final OrganizacionService servicio;
    private final ActorSesion actor;

    PerfilController(OrganizacionService servicio, ActorSesion actor) {
        this.servicio = servicio;
        this.actor = actor;
    }

    @GetMapping(Rutas.ORG_PERFIL)
    String perfil(Model modelo) {
        modelo.addAttribute("form", PerfilForm.de(servicio.actual(actor.actual())));
        return formulario(modelo);
    }

    @PostMapping(Rutas.ORG_PERFIL)
    String guardar(@Valid @ModelAttribute("form") PerfilForm form, BindingResult errores,
            Model modelo, RedirectAttributes redir) {
        if (errores.hasErrors()) {
            return formulario(modelo);
        }
        var org = servicio.actual(actor.actual());
        if (!servicio.actualizarPerfil(org.id(), form.getMateriales(),
                form.getHorario(), form.getTelefono(), form.getVersion())) {
            modelo.addAttribute("conflicto", true);
            return formulario(modelo);
        }
        redir.addFlashAttribute("guardado", true);
        return "redirect:" + Rutas.ORG_PERFIL;
    }

    private String formulario(Model modelo) {
        modelo.addAttribute("materialesTodos", Material.values());
        return "secciones/org/perfil";
    }
}
