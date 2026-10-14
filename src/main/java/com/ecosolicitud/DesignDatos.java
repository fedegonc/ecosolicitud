package com.ecosolicitud;

import java.util.Arrays;
import java.util.List;

// Contenido estático de /design por ciudad. Las cadenas de texto son claves
// de messages (el template las resuelve con #{...}); nombres propios sin traducir.
final class DesignDatos {

    record Tarjeta(String clave, String imagen, String enlace) {
    }

    record Herramienta(String icono, String clave, String enlace) {
    }

    record Aviso(String clave) {
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

    static Pagina para(String ciudad) {
        return switch (ciudad) {
            case "livramento" -> livramento();
            case "tranqueras" -> tranqueras();
            case "minas" -> minas();
            case "vichadero" -> vichadero();
            default -> rivera();
        };
    }

    static Pagina rivera() {
        return new Pagina("rivera", "Rivera", "ds.titulo", "ds.subtitulo.rivera",
                divulgacion(),
                List.of(
                        new Herramienta("basurero", "ds.h.residuo", "/guia"),
                        new Herramienta("pin", "ds.h.lugar", "/acopios"),
                        new Herramienta("calendario", "ds.h.dia", "/guia"),
                        new Herramienta("corazon", "ds.h.llevar", "/acopios")),
                List.of(),
                List.of(new Actor("Renacer del Norte"), new Actor("Plan Vale"),
                        new Actor("Intendencia"), new Actor("Campo Limpio")));
    }

    static Pagina livramento() {
        return new Pagina("livramento", "Livramento", "ds.titulo", "ds.subtitulo.livramento",
                divulgacion(),
                List.of(
                        new Herramienta("camion", "ds.h.retiro", "/nueva"),
                        new Herramienta("corazon", "ds.h.donar", "/nueva"),
                        new Herramienta("pin", "ds.h.lugar", "/acopios"),
                        new Herramienta("basurero", "ds.h.residuo", "/guia")),
                List.of(new Aviso("ds.aviso.retiro")),
                List.of(new Actor("Novo Horizonte"), new Actor("Prefeitura")));
    }

    static Pagina tranqueras() {
        return secundaria("tranqueras", "Tranqueras");
    }

    static Pagina minas() {
        return secundaria("minas", "Minas de Corrales");
    }

    static Pagina vichadero() {
        return secundaria("vichadero", "Vichadero");
    }

    private static Pagina secundaria(String ciudad, String nombre) {
        return new Pagina(ciudad, nombre, "ds.titulo", "ds.subtitulo.secundaria",
                divulgacion(), List.of(new Herramienta("corazon", "ds.h.llevar", "/acopios")),
                List.of(), List.of(new Actor("Intendencia")));
    }

    private static List<Tarjeta> divulgacion() {
        return List.of(
                new Tarjeta("ds.t1", "/img/comunidad/rivera-verde-800.webp", "/guia"),
                new Tarjeta("ds.t2", "/img/comunidad/novo-horizonte-800.webp", "/guia"),
                new Tarjeta("ds.t3", "/img/comunidad/punto-verde-800.webp", "/guia"),
                new Tarjeta("ds.t4", "/img/comunidad/acopio-800.webp", "/guia"));
    }
}
