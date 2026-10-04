package com.ecosolicitud.comunidad.interno;

import com.ecosolicitud.comunidad.ComunidadService;
import com.ecosolicitud.comunidad.FiltroComunidad;
import com.ecosolicitud.shared.Rutas;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
class ComunidadController {

    private final ComunidadService comunidad;

    ComunidadController(ComunidadService comunidad) {
        this.comunidad = comunidad;
    }

    @GetMapping(Rutas.COMUNIDAD)
    String lista(@RequestParam(required = false) String tipo, Model modelo) {
        var activo = FiltroComunidad.desde(tipo);
        modelo.addAttribute("filtro", activo);
        modelo.addAttribute("filtros", FiltroComunidad.values());
        modelo.addAttribute("publicaciones", comunidad.publicadas(activo));
        return "secciones/comunidad";
    }

    @GetMapping(Rutas.COMUNIDAD + "/{id}")
    String publicacion(@PathVariable long id, Model modelo) {
        modelo.addAttribute("publicacion", comunidad.buscar(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "secciones/comunidad-publicacion";
    }
}
