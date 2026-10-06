package com.ecosolicitud.solicitud.interno;

import java.util.List;
import java.util.Optional;

import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.solicitud.SolicitudCreacion;
import com.ecosolicitud.solicitud.SolicitudService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class NuevaController {

    private final SolicitudService solicitudes;
    private final OrganizacionService organizaciones;
    private final ActorSesion actor;

    NuevaController(SolicitudService solicitudes, OrganizacionService organizaciones,
            ActorSesion actor) {
        this.solicitudes = solicitudes;
        this.organizaciones = organizaciones;
        this.actor = actor;
    }

    @GetMapping(Rutas.NUEVA)
    String nueva(@ModelAttribute("form") NuevaForm form, Model modelo) {
        if (form.getNombre() == null || form.getNombre().isBlank()) {
            form.setNombre(actor.actual().nombreCiudadano());
        }
        var centros = centrosDe(form);
        var centro = centroElegido(centros, form);
        centro.ifPresentOrElse(o -> form.getMateriales().retainAll(o.materiales()
                        .stream().filter(Material::isActivo).toList()),
                () -> form.getMateriales().clear());
        return formulario(modelo, form, centros, centro);
    }

    @PostMapping(Rutas.NUEVA)
    String crear(@Valid @ModelAttribute("form") NuevaForm form, BindingResult errores,
            Model modelo, RedirectAttributes redir) {
        if (errores.getFieldErrors().stream().anyMatch(FieldError::isBindingFailure)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        var centros = centrosDe(form);
        var centro = centroElegido(centros, form);
        if (!errores.hasErrors() && centro.isEmpty()) {
            errores.rejectValue("organizacionId", "nueva.error.centro");
        }
        // misma regla que el service (RN-04), descompuesta para dar el error por campo
        if (!errores.hasErrors()
                && !centro.get().recibeTodos(form.getMateriales())) {
            errores.rejectValue("materiales", "nueva.error.material.no-recibido");
        }
        if (!errores.hasErrors()) {
            var datos = new SolicitudCreacion(form.getCiudad(), form.getDireccion(),
                    form.getReferencia(), form.getMateriales(), form.getOrganizacionId(),
                    form.getNombre(), form.getContacto(), form.getNota());
            var creada = solicitudes.crear(actor.actual(), datos);
            if (creada.isPresent()) {
                redir.addFlashAttribute("enviada", creada.get().id());
                redir.addFlashAttribute("enviadaA", creada.get().organizacionNombre());
                return "redirect:" + Rutas.MIS_SOLICITUDES;
            }
            errores.rejectValue("organizacionId", "nueva.error.organizacion");
        }
        return formulario(modelo, form, centros, centro);
    }

    private List<OrganizacionInfo> centrosDe(NuevaForm form) {
        return form.getCiudad() == null ? List.of() : organizaciones.enCiudad(form.getCiudad());
    }

    private Optional<OrganizacionInfo> centroElegido(
            List<OrganizacionInfo> centros, NuevaForm form) {
        return centros.stream().filter(o -> o.id().equals(form.getOrganizacionId())).findFirst();
    }

    private String formulario(Model modelo, NuevaForm form,
            List<OrganizacionInfo> centros, Optional<OrganizacionInfo> centro) {
        modelo.addAttribute("form", form);
        modelo.addAttribute("ciudades", Ciudad.values());
        modelo.addAttribute("centros", centros);
        modelo.addAttribute("centro", centro.orElse(null));
        modelo.addAttribute("materialesCentro", centro.map(o -> o.materiales()
                .stream().filter(Material::isActivo).toList()).orElse(List.of()));
        return "secciones/nueva";
    }
}
