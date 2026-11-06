package com.ecosolicitud;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.ecosolicitud.shared.CatalogoMateriales;
import com.ecosolicitud.shared.Miga;
import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.solicitud.AvisoInfo;
import com.ecosolicitud.solicitud.Estado;
import com.ecosolicitud.solicitud.SolicitudInfo;
import com.ecosolicitud.solicitud.TipoAviso;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    String catalogo(@RequestParam(defaultValue = "1") int pagina, Locale idioma, Model modelo) {
        var ahora = Instant.parse("2026-01-15T12:00:00Z");
        var centro = mensajes.getMessage("catalogo.centro", null, idioma);
        var direccion = mensajes.getMessage("catalogo.direccion", null, idioma);
        var seleccion = materiales.activos();
        var ejemplos = Arrays.stream(Estado.values()).map(e -> {
            var solicitud = new SolicitudInfo(e.ordinal() + 1, direccion, null, null,
                    seleccion.subList(0, Math.min(2, seleccion.size())), centro, "Ana",
                    "", null, e, ahora, e.esFinal() ? ahora.plusSeconds(3600) : null, 0);
            var acciones = new ArrayList<String>();
            if (e.permiteAceptar()) acciones.add("accion.aceptar");
            if (e.permiteCompletar()) acciones.add("accion.completar");
            if (e.permiteRechazar()) acciones.add("accion.rechazar");
            if (e.permiteCancelar()) acciones.add("accion.cancelar");
            return new Ejemplo(solicitud, List.copyOf(acciones));
        }).toList();
        int indice = Math.max(1, Math.min(3, pagina)) - 1;
        int desde = indice * 2;
        modelo.addAttribute("pagina", new PageImpl<>(
                ejemplos.subList(desde, Math.min(desde + 2, ejemplos.size())),
                PageRequest.of(indice, 2), ejemplos.size()));
        modelo.addAttribute("estados", Estado.values());
        modelo.addAttribute("materialesMuestra", seleccion);
        modelo.addAttribute("tokens", List.of("--acento", "--acento-texto", "--superficie",
                "--fondo", "--texto", "--texto-secundario", "--borde", "--peligro",
                "--color-realce", "--color-atencion", "--color-exito"));
        modelo.addAttribute("tipos", List.of("--t-xs", "--t-sm", "--t-base", "--t-lg", "--t-xl"));
        modelo.addAttribute("ritmos", List.of("--chico", "--gap-l", "--pad", "--pad-grande",
                "--radio", "--toque-min", "--panel-w", "--medida"));
        modelo.addAttribute("iconos", List.of("persona", "pin", "camion", "reloj", "tilde", "cruz"));
        modelo.addAttribute("columnasMuestra", List.of(
                new Columna("org.grupo.abiertas", ejemplos.stream()
                        .filter(e -> !e.solicitud().estado().esFinal()).limit(1).toList()),
                new Columna("org.grupo.cerradas", List.of())));
        modelo.addAttribute("avisosMuestra", List.of(
                new AvisoInfo(1, TipoAviso.ACEPTADA, centro, null, false, ahora, Estado.EN_CURSO),
                new AvisoInfo(2, TipoAviso.COMPLETADA, centro, null, true, ahora, Estado.COMPLETADA)));
        modelo.addAttribute("migas", List.of(Miga.inicio(), Miga.actualClave("catalogo.titulo")));
        return "dev/design-system";
    }

    public record Ejemplo(SolicitudInfo solicitud, List<String> acciones) {
    }

    public record Columna(String clave, List<Ejemplo> ejemplos) {
    }
}
