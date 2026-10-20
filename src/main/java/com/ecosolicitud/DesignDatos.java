package com.ecosolicitud;

import java.util.List;

import com.ecosolicitud.guia.GuiaService;
import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.shared.Rutas;

// Contenido estático de la portada por rol y ciudad. Las cadenas de texto son
// claves de messages (el template las resuelve con #{...}); nombres propios sin traducir.
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

    static Pagina para(Localidad localidad, Rol rol, GuiaService guia) {
        return rol == Rol.ORGANIZACION
                ? new Pagina(localidad, "ds.titulo.org", "ds.subtitulo.org",
                        accionesOrganizacion(), consejos(guia))
                : new Pagina(localidad, "ds.titulo", subtituloVecino(localidad),
                        accionesVecino(localidad, guia), consejos(guia));
    }

    private static String subtituloVecino(Localidad localidad) {
        return switch (localidad) {
            case RIVERA -> "ds.subtitulo.rivera";
            case LIVRAMENTO -> "ds.subtitulo.livramento";
        };
    }

    // el vecino: pedir, saber qué hacer y adónde llevarlo; la cuarta depende de
    // quién gestiona la ciudad (Intendencia: voluminosos por calendario;
    // Prefeitura: central de poda)
    private static List<Accion> accionesVecino(Localidad localidad, GuiaService guia) {
        var propia = switch (localidad) {
            case RIVERA -> new Accion("calendario", "ds.accion.voluminosos",
                    articulo(guia, "Muebles y voluminosos"));
            case LIVRAMENTO -> new Accion("hoja", "ds.accion.poda",
                    articulo(guia, "Orgánicos y restos de poda"));
        };
        return List.of(
                new Accion("camion", "ds.accion.retiro", Rutas.NUEVA),
                new Accion("libro", "ds.accion.material", Rutas.GUIA),
                new Accion("pin", "ds.accion.llevar", Rutas.ACOPIOS),
                propia,
                new Accion("gente", "ds.accion.recicladores", Rutas.COMUNIDAD));
    }

    // el centro: su trabajo del día primero, después su vidriera y sus números
    private static List<Accion> accionesOrganizacion() {
        return List.of(
                new Accion("bandeja", "ds.accion.recibidas", Rutas.ORG_SOLICITUDES),
                new Accion("edificio", "ds.accion.centro", Rutas.ORG_PERFIL),
                new Accion("grafico", "ds.accion.informe", Rutas.ORG_INFORME),
                new Accion("campana", "ds.accion.avisos", Rutas.AVISOS),
                new Accion("libro", "ds.accion.guia", Rutas.GUIA));
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
