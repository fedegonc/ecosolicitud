package com.ecosolicitud.organizacion.interno;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.ecosolicitud.shared.Material;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.organizacion.AcopiosCiudad;
import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.organizacion.OrganizacionService;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class AcopiosController {

    // el buscador "¿Dónde llevo esto?" de la portada pide solo los primeros lugares
    private static final String PORTADA_LUGARES = "portada-lugares";
    private static final int LUGARES_PORTADA = 3;

    private final OrganizacionService servicio;
    private final MessageSource mensajes;

    AcopiosController(OrganizacionService servicio, MessageSource mensajes) {
        this.servicio = servicio;
        this.mensajes = mensajes;
    }

    @GetMapping(Rutas.ACOPIOS)
    String acopios(@RequestParam(required = false) String material,
            @RequestHeader(name = "HX-Target", required = false) String destino,
            Model modelo, Locale locale) {
        var buscado = normalizar(material);
        var grupos = servicio.porCiudad().stream()
                .map(g -> new AcopiosCiudad(g.ciudad(), g.organizaciones().stream()
                        .filter(o -> recibe(o, buscado, locale)).toList()))
                .toList();
        modelo.addAttribute("material", buscado.isEmpty() ? null : material.strip());
        if (PORTADA_LUGARES.equals(destino)) {
            modelo.addAttribute("lugares", buscado.isEmpty() ? List.of() : grupos.stream()
                    .flatMap(g -> g.organizaciones().stream()).limit(LUGARES_PORTADA).toList());
            return "portada :: lugares";
        }
        modelo.addAttribute("grupos", grupos);
        return "secciones/acopios";
    }

    // "botellas de plástico" encuentra Plástico y "vidro" encuentra Vidrio en portugués:
    // se compara sin tildes contra el código, el nombre y el nombre en el idioma del vecino
    private boolean recibe(OrganizacionInfo centro, String buscado, Locale locale) {
        return buscado.isEmpty() || centro.materiales().stream().filter(Material::isActivo)
                .flatMap(m -> Stream.of(m.getCodigo(), m.getNombre(), mensajes.getMessage(
                        "material." + m.getCodigo().toLowerCase(Locale.ROOT), null, m.getNombre(), locale)))
                .map(AcopiosController::normalizar)
                .anyMatch(n -> n.contains(buscado) || buscado.contains(n));
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
