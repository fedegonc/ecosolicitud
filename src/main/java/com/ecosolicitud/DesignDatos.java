package com.ecosolicitud;

import java.util.Arrays;
import java.util.List;

import com.ecosolicitud.comunidad.ComunidadService;
import com.ecosolicitud.guia.GuiaService;
import com.ecosolicitud.shared.Rutas;

// Contenido estático de /design por ciudad. Las cadenas de texto son claves
// de messages (el template las resuelve con #{...}); nombres propios sin traducir.
final class DesignDatos {

    record Tarjeta(String clave, String imagen, String enlace) {
    }

    record Herramienta(String icono, String clave, String enlace) {
    }

    // publicacion: título exacto de la publicación a la que enlaza el banner, null si enlaza a /avisos
    record Aviso(String clave, String publicacion) {
    }

    record Actor(String nombre) {

        // hasta 2 letras: primera de cada palabra ("Renacer del Norte" -> RN);
        // una sola palabra toma sus dos primeras ("Intendencia" -> IN)
        public String getIniciales() {
            var palabras = Arrays.stream(nombre.split(" "))
                    .filter(p -> !List.of("de", "del", "la", "y").contains(p.toLowerCase()))
                    .toList();
            if (palabras.size() > 1) {
                return ("" + palabras.get(0).charAt(0) + palabras.get(1).charAt(0)).toUpperCase();
            }
            return nombre.substring(0, 2).toUpperCase();
        }
    }

    record Pagina(Localidad localidad, String titulo, String subtitulo,
            List<Tarjeta> divulgacion, List<Herramienta> recoleccion,
            List<Aviso> avisos, List<Actor> actores) {

        // el selector de la portada recorre todas
        public List<Localidad> getLocalidades() {
            return Localidad.todas();
        }
    }

    private DesignDatos() {
    }

    static Pagina para(String ciudad, GuiaService guia) {
        var localidad = Localidad.desde(ciudad);
        return switch (localidad) {
            case RIVERA -> rivera(guia);
            case LIVRAMENTO -> livramento(guia);
            case TRANQUERAS, MINAS, VICHADERO -> secundaria(localidad, guia);
        };
    }

    static Pagina rivera(GuiaService guia) {
        return new Pagina(Localidad.RIVERA, "ds.titulo", "ds.subtitulo.rivera",
                divulgacion(guia),
                List.of(
                        new Herramienta("basurero", "ds.h.residuo", Rutas.GUIA),
                        new Herramienta("pin", "ds.h.lugar", Rutas.ACOPIOS),
                        new Herramienta("calendario", "ds.h.dia",
                                articulo(guia, "Sacarlo de casa")),
                        new Herramienta("corazon", "ds.h.llevar", Rutas.ACOPIOS)),
                List.of(new Aviso("ds.aviso.puntoverde",
                        "Nuevo Punto Verde en el Polo de Desarrollo")),
                List.of(new Actor("Renacer del Norte"), new Actor("Plan Vale"),
                        new Actor("Intendencia"), new Actor("Campo Limpio")));
    }

    static Pagina livramento(GuiaService guia) {
        return new Pagina(Localidad.LIVRAMENTO, "ds.titulo", "ds.subtitulo.livramento",
                divulgacion(guia),
                List.of(
                        new Herramienta("camion", "ds.h.retiro", Rutas.NUEVA),
                        new Herramienta("corazon", "ds.h.donar", Rutas.NUEVA),
                        new Herramienta("pin", "ds.h.lugar", Rutas.ACOPIOS),
                        new Herramienta("basurero", "ds.h.residuo", Rutas.GUIA)),
                List.of(new Aviso("ds.aviso.retiro", null)),
                List.of(new Actor("Novo Horizonte"), new Actor("Prefeitura")));
    }

    private static Pagina secundaria(Localidad localidad, GuiaService guia) {
        return new Pagina(localidad, "ds.titulo", "ds.subtitulo.secundaria",
                divulgacion(guia), List.of(new Herramienta("corazon", "ds.h.llevar", Rutas.ACOPIOS)),
                List.of(), List.of(new Actor("Intendencia")));
    }

    // cada tarjeta enlaza a su artículo de la guía; si no existe, a la guía general
    private static List<Tarjeta> divulgacion(GuiaService guia) {
        return List.of(
                new Tarjeta("ds.t1", "/img/comunidad/rivera-verde-800.webp",
                        articulo(guia, "Antes de tirar")),
                new Tarjeta("ds.t2", "/img/comunidad/novo-horizonte-800.webp",
                        articulo(guia, "Cómo separar en casa")),
                new Tarjeta("ds.t3", "/img/comunidad/punto-verde-800.webp",
                        articulo(guia, "Sacarlo de casa")),
                new Tarjeta("ds.t4", "/img/comunidad/acopio-800.webp",
                        articulo(guia, "Qué pasa después")));
    }

    private static String articulo(GuiaService guia, String titulo) {
        return guia.buscarPorTitulo(titulo).map(a -> Rutas.expandir(Rutas.GUIA_ARTICULO, a.id())).orElse(Rutas.GUIA);
    }

    // el banner enlaza a su publicación cuando la hay; si no, a la bandeja de avisos
    static String enlaceAviso(Pagina pagina, ComunidadService comunidad) {
        if (pagina.avisos().isEmpty() || pagina.avisos().get(0).publicacion() == null) {
            return Rutas.AVISOS;
        }
        return comunidad.buscarPorTitulo(pagina.avisos().get(0).publicacion())
                .map(p -> Rutas.expandir(Rutas.COMUNIDAD_PUBLICACION, p.id())).orElse(Rutas.AVISOS);
    }
}
