package com.ecosolicitud.solicitud.interno;

import java.util.List;

import com.ecosolicitud.Rutas;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class NuevaController {

    private final SolicitudService solicitudes;
    private final OrganizacionService organizaciones;

    NuevaController(SolicitudService solicitudes, OrganizacionService organizaciones) {
        this.solicitudes = solicitudes;
        this.organizaciones = organizaciones;
    }

    @GetMapping(Rutas.NUEVA)
    String nueva(@RequestParam(required = false) Ciudad ciudad,
            @RequestParam(required = false) List<Material> material,
            @RequestParam(required = false) String organizacion, Model modelo) {
        if (ciudad == null || material == null || material.isEmpty()) {
            var form = new NuevaForm();
            if (organizacion != null && !organizacion.isBlank()) {
                form.setOrganizacionId(organizacion);
                organizaciones.buscar(organizacion)
                        .ifPresent(o -> form.setCiudad(o.ciudad()));
            }
            return formulario(modelo, form, false);
        }
        var compatibles = organizaciones.compatibles(ciudad, material);
        var form = new NuevaForm();
        form.setCiudad(ciudad);
        form.setMateriales(material);
        if (organizacion != null && compatibles.stream()
                .noneMatch(o -> o.id().equals(organizacion))) {
            modelo.addAttribute("preseleccionCaida", true);
        } else {
            form.setOrganizacionId(organizacion);
        }
        modelo.addAttribute("compatibles", compatibles);
        return formulario(modelo, form, true);
    }

    @PostMapping(Rutas.NUEVA)
    String crear(@Valid @ModelAttribute("form") NuevaForm form, BindingResult errores,
            Model modelo, RedirectAttributes redir) {
        if (errores.getFieldErrors().stream().anyMatch(FieldError::isBindingFailure)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (!errores.hasErrors()) {
            var creada = solicitudes.crear(form.getCiudad(),
                    form.getDireccion(), form.getReferencia(), form.getMateriales(),
                    form.getOrganizacionId(), form.getNota());
            if (creada.isPresent()) {
                redir.addFlashAttribute("enviada", creada.get().id());
                redir.addFlashAttribute("enviadaA", creada.get().organizacionNombre());
                return "redirect:" + Rutas.MIS_SOLICITUDES;
            }
            errores.rejectValue("organizacionId", "nueva.error.organizacion");
        }
        boolean conBusqueda = form.getCiudad() != null && !form.getMateriales().isEmpty();
        modelo.addAttribute("compatibles", conBusqueda
                ? organizaciones.compatibles(form.getCiudad(), form.getMateriales())
                : List.of());
        return formulario(modelo, form, conBusqueda);
    }

    private String formulario(Model modelo, NuevaForm form, boolean busqueda) {
        modelo.addAttribute("form", form);
        modelo.addAttribute("busqueda", busqueda);
        modelo.addAttribute("ciudades", Ciudad.values());
        modelo.addAttribute("materialesTodos", Material.values());
        return "secciones/nueva";
    }
}
