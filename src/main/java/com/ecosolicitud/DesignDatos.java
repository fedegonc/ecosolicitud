package com.ecosolicitud;

import java.util.List;

import com.ecosolicitud.comunidad.ComunidadService;
import com.ecosolicitud.guia.GuiaService;
import com.ecosolicitud.shared.Rutas;

// Contenido de la portada por localidad. Las cadenas de texto son claves
// de messages (el template las resuelve con #{...}).
final class DesignDatos {

    // la respuesta a "¿Cuándo pasa el camión?" con el dato que hoy existe
    record Respuesta(String clave, String enlace) {
    }

    record Pregunta(String clave, String enlace) {
    }

    // publicacion: título exacto de la publicación a la que enlaza, null si enlaza a /avisos
    record Aviso(String clave, String publicacion) {
    }

    // respuesta y aviso son null en las localidades sin información publicada
    record Pagina(Localidad localidad, Respuesta respuesta, List<Pregunta> preguntas, Aviso aviso) {

        // el selector de la portada recorre todas
        public List<Localidad> getLocalidades() {
            return Localidad.todas();
        }
    }

    private DesignDatos() {
    }

    static Pagina para(String ciudad, GuiaService guia) {
        var localidad = Localidad.desde(ciudad);
        var voluminosos = articulo(guia, "Muebles y voluminosos");
        var separar = new Pregunta("portada.separar", Rutas.GUIA);
        return switch (localidad) {
            case RIVERA -> new Pagina(localidad,
                    new Respuesta("portada.cuando.rivera", voluminosos),
                    List.of(separar),
                    new Aviso("ds.aviso.puntoverde", "Nuevo Punto Verde en el Polo de Desarrollo"));
            // Livramento es la ciudad donde se pide el retiro
            case LIVRAMENTO -> new Pagina(localidad,
                    new Respuesta("portada.cuando.livramento", voluminosos),
                    List.of(separar, new Pregunta("portada.retiro", Rutas.NUEVA)),
                    new Aviso("ds.aviso.retiro", null));
            case TRANQUERAS, MINAS, VICHADERO -> new Pagina(localidad, null, List.of(separar), null);
        };
    }

    // enlaza al artículo de la guía; si no existe, a la guía general
    private static String articulo(GuiaService guia, String titulo) {
        return guia.buscarPorTitulo(titulo).map(a -> Rutas.expandir(Rutas.GUIA_ARTICULO, a.id())).orElse(Rutas.GUIA);
    }

    // el aviso enlaza a su publicación cuando la hay; si no, a la bandeja de avisos
    static String enlaceAviso(Pagina pagina, ComunidadService comunidad) {
        if (pagina.aviso() == null || pagina.aviso().publicacion() == null) {
            return Rutas.AVISOS;
        }
        return comunidad.buscarPorTitulo(pagina.aviso().publicacion())
                .map(p -> Rutas.expandir(Rutas.COMUNIDAD_PUBLICACION, p.id())).orElse(Rutas.AVISOS);
    }
}
