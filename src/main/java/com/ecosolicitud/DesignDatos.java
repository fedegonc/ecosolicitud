package com.ecosolicitud;

import java.util.List;

import com.ecosolicitud.guia.GuiaService;
import com.ecosolicitud.shared.Rutas;

// Contenido estático de la portada por ciudad. Las cadenas de texto son claves
// de messages (el template las resuelve con #{...}); nombres propios sin traducir.
final class DesignDatos {

    record Tarjeta(String clave, String imagen, String enlace) {
    }

    record Accion(String icono, String clave, String enlace) {
    }

    record Pagina(Localidad localidad, String titulo, String subtitulo,
            List<Accion> acciones, List<Tarjeta> consejos) {

        // el selector de ciudad recorre todas
        public List<Localidad> getLocalidades() {
            return Localidad.todas();
        }
    }

    private DesignDatos() {
    }

    static Pagina para(String ciudad, GuiaService guia) {
        var localidad = Localidad.desde(ciudad);
        var subtitulo = switch (localidad) {
            case RIVERA -> "ds.subtitulo.rivera";
            case LIVRAMENTO -> "ds.subtitulo.livramento";
        };
        return new Pagina(localidad, "ds.titulo", subtitulo, acciones(guia), consejos(guia));
    }

    // lo que el vecino puede hacer: primero y al centro, igual en toda localidad
    private static List<Accion> acciones(GuiaService guia) {
        return List.of(
                new Accion("camion", "ds.h.retiro", Rutas.NUEVA),
                new Accion("basurero", "ds.h.residuo", Rutas.GUIA),
                new Accion("pin", "ds.h.lugar", Rutas.ACOPIOS),
                new Accion("calendario", "ds.h.dia", articulo(guia, "Muebles y voluminosos")),
                new Accion("corazon", "ds.h.donar", Rutas.ACOPIOS));
    }

    // cada tarjeta enlaza a su artículo de la guía; si no existe, a la guía general
    private static List<Tarjeta> consejos(GuiaService guia) {
        return List.of(
                new Tarjeta("ds.t1", "/img/comunidad/rivera-verde-800.webp",
                        articulo(guia, "Antes de tirar")),
                new Tarjeta("ds.t2", "/img/comunidad/novo-horizonte-800.webp",
                        articulo(guia, "Cómo separar en casa")),
                new Tarjeta("ds.t3", "/img/comunidad/punto-verde-800.webp",
                        articulo(guia, "Sacarlo de casa")));
    }

    private static String articulo(GuiaService guia, String titulo) {
        return guia.buscarPorTitulo(titulo).map(a -> Rutas.expandir(Rutas.GUIA_ARTICULO, a.id())).orElse(Rutas.GUIA);
    }
}
