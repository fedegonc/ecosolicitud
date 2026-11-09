package com.ecosolicitud;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import com.ecosolicitud.shared.CatalogoMateriales;
import com.ecosolicitud.shared.Miga;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.solicitud.AvisoInfo;
import com.ecosolicitud.solicitud.ColumnaAvisos;
import com.ecosolicitud.solicitud.Estado;
import com.ecosolicitud.solicitud.SolicitudInfo;
import com.ecosolicitud.solicitud.TipoAviso;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// Muestras efímeras: lee el catálogo real, pero no crea cuentas ni solicitudes.
@Controller
@Profile("dev & !prod")
class DesignSystemController {

    private final CatalogoMateriales materiales;
    private final MessageSource mensajes;

    DesignSystemController(CatalogoMateriales materiales, MessageSource mensajes) {
        this.materiales = materiales;
        this.mensajes = mensajes;
    }

    @GetMapping(Rutas.DEV_DESIGN_SYSTEM)
    String catalogo(Locale idioma, Model modelo) {
        var ahora = Instant.parse("2026-01-15T12:00:00Z");
        var centro = mensajes.getMessage("catalogo.centro", null, idioma);
        var direccion = mensajes.getMessage("catalogo.direccion", null, idioma);
        var seleccion = materiales.activos();
        modelo.addAttribute("solicitudMuestra", new SolicitudInfo(1, direccion, null, null,
                seleccion.subList(0, Math.min(2, seleccion.size())), centro, "Ana",
                "", null, Estado.PENDIENTE, ahora, null, 0));
        modelo.addAttribute("estados", Estado.values());
        modelo.addAttribute("materialesMuestra", seleccion);
        modelo.addAttribute("tokens", List.of("--acento", "--acento-texto", "--superficie",
                "--fondo", "--texto", "--texto-secundario", "--borde", "--peligro",
                "--color-realce", "--color-atencion", "--color-exito"));
        modelo.addAttribute("tipos", List.of("--t-xs", "--t-sm", "--t-base", "--t-lg", "--t-xl"));
        modelo.addAttribute("ritmos", List.of("--chico", "--gap-l", "--pad", "--pad-grande",
                "--radio", "--toque-min", "--panel-w", "--medida"));
        var aviso = new AvisoInfo(1, TipoAviso.NUEVA, centro, null, false, ahora, Estado.PENDIENTE);
        modelo.addAttribute("columnasMuestra", List.of(
                new ColumnaAvisos("org.grupo.abiertas", List.of(aviso)),
                new ColumnaAvisos("org.grupo.cerradas", List.of())));
        modelo.addAttribute("avisosMuestra", List.of(
                new AvisoInfo(1, TipoAviso.ACEPTADA, centro, null, false, ahora, Estado.EN_CURSO),
                new AvisoInfo(2, TipoAviso.COMPLETADA, centro, null, true, ahora, Estado.COMPLETADA)));
        modelo.addAttribute("migas", List.of(Miga.inicio(), Miga.actualClave("catalogo.titulo")));
        return "dev/design-system";
    }
}
