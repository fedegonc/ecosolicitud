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

    record Pagina(String ciudad, String nombre, String titulo, String subtitulo,
            List<Tarjeta> divulgacion, List<Herramienta> recoleccion,
            List<Aviso> avisos, List<Actor> actores) {
    }

    private DesignDatos() {
    }

    static Pagina para(String ciudad, GuiaService guia) {
        return switch (ciudad) {
            case "livramento" -> livramento(guia);
            case "tranqueras" -> tranqueras(guia);
            case "minas" -> minas(guia);
            case "vichadero" -> vichadero(guia);
            default -> rivera(guia);
        };
    }

    static Pagina rivera(GuiaService guia) {
        return new Pagina("rivera", "Rivera", "ds.titulo", "ds.subtitulo.rivera",
                divulgacion(guia),
                List.of(
                        new Herramienta("basurero", "ds.h.residuo", "/guia"),
                        new Herramienta("pin", "ds.h.lugar", "/acopios"),
                        new Herramienta("calendario", "ds.h.dia", "/guia"),
                        new Herramienta("corazon", "ds.h.llevar", "/acopios")),
                List.of(new Aviso("ds.aviso.puntoverde",
                        "Nuevo Punto Verde en el Polo de Desarrollo")),
                List.of(new Actor("Renacer del Norte"), new Actor("Plan Vale"),
                        new Actor("Intendencia"), new Actor("Campo Limpio")));
    }

    static Pagina livramento(GuiaService guia) {
        return new Pagina("livramento", "Livramento", "ds.titulo", "ds.subtitulo.livramento",
                divulgacion(guia),
                List.of(
                        new Herramienta("camion", "ds.h.retiro", "/nueva"),
                        new Herramienta("corazon", "ds.h.donar", "/nueva"),
                        new Herramienta("pin", "ds.h.lugar", "/acopios"),
                        new Herramienta("basurero", "ds.h.residuo", "/guia")),
                List.of(new Aviso("ds.aviso.retiro", null)),
                List.of(new Actor("Novo Horizonte"), new Actor("Prefeitura")));
    }

    static Pagina tranqueras(GuiaService guia) {
        return secundaria("tranqueras", "Tranqueras", guia);
    }

    static Pagina minas(GuiaService guia) {
        return secundaria("minas", "Minas de Corrales", guia);
    }

    static Pagina vichadero(GuiaService guia) {
        return secundaria("vichadero", "Vichadero", guia);
    }

    private static Pagina secundaria(String ciudad, String nombre, GuiaService guia) {
        return new Pagina(ciudad, nombre, "ds.titulo", "ds.subtitulo.secundaria",
                divulgacion(guia), List.of(new Herramienta("corazon", "ds.h.llevar", "/acopios")),
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
        return guia.buscarPorTitulo(titulo).map(a -> Rutas.GUIA + "/" + a.id()).orElse(Rutas.GUIA);
    }

    // el banner enlaza a su publicación cuando la hay; si no, a la bandeja de avisos
    static String enlaceAviso(Pagina pagina, ComunidadService comunidad) {
        if (pagina.avisos().isEmpty() || pagina.avisos().get(0).publicacion() == null) {
            return Rutas.AVISOS;
        }
        return comunidad.buscarPorTitulo(pagina.avisos().get(0).publicacion())
                .map(p -> Rutas.COMUNIDAD + "/" + p.id()).orElse(Rutas.AVISOS);
    }
}
