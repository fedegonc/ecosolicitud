package com.ecosolicitud;

import java.util.List;

import com.ecosolicitud.comunidad.ComunidadService;
import com.ecosolicitud.shared.Rutas;

// Contenido de la portada por localidad. Las cadenas de texto son claves
// de messages (el template las resuelve con #{...}).
final class DesignDatos {

    record Pregunta(String clave, String enlace) {
    }

    // publicacion: título exacto de la publicación a la que enlaza, null si enlaza a /avisos
    record Aviso(String clave, String publicacion) {
    }

    // aviso es null en las localidades sin información publicada
    record Pagina(Localidad localidad, List<Pregunta> preguntas, Aviso aviso) {

        // el selector de la portada recorre todas
        public List<Localidad> getLocalidades() {
            return Localidad.todas();
        }

        public List<Recoleccion.Zona> getZonas() {
            return Recoleccion.zonas(localidad);
        }
    }

    private DesignDatos() {
    }

    static Pagina para(String ciudad) {
        var localidad = Localidad.desde(ciudad);
        var separar = new Pregunta("portada.separar", Rutas.GUIA);
        return switch (localidad) {
            case RIVERA -> new Pagina(localidad, List.of(separar),
                    new Aviso("ds.aviso.puntoverde", "Nuevo Punto Verde en el Polo de Desarrollo"));
            // Livramento es la ciudad donde se pide el retiro
            case LIVRAMENTO -> new Pagina(localidad, List.of(separar, new Pregunta("portada.retiro", Rutas.NUEVA)),
                    new Aviso("ds.aviso.retiro", null));
            case TRANQUERAS, MINAS, VICHADERO -> new Pagina(localidad, List.of(separar), null);
        };
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
