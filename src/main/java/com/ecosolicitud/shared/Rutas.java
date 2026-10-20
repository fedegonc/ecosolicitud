package com.ecosolicitud.shared;

// Único espacio de URLs: controllers (@GetMapping/@PostMapping/redirect),
// plantillas (${rutas.X}, expuesto por NavegacionAdvice) y tests.
// Las rutas con {id} son plantillas: Thymeleaf las completa con @{${rutas.X}(id=...)}
// y los tests con las variables de URI de TestRestTemplate/MockMvc.
public final class Rutas {

    public static final String RAIZ = "/";

    public static final String NUEVA = "/nueva";
    public static final String NUEVA_VALIDAR = NUEVA + "/validar";
    public static final String MIS_SOLICITUDES = "/mis-solicitudes";
    public static final String MIS_SOLICITUD = MIS_SOLICITUDES + "/{id}";
    public static final String MIS_CANCELAR = MIS_SOLICITUD + "/cancelar";
    public static final String MIS_INFORME = MIS_SOLICITUDES + "/informe";
    public static final String ACOPIOS = "/acopios";
    public static final String GUIA = "/guia";
    public static final String GUIA_ARTICULO = GUIA + "/{id}";
    public static final String COMUNIDAD = "/comunidad";
    public static final String COMUNIDAD_PUBLICACION = COMUNIDAD + "/{id}";
    public static final String ESTADISTICAS = "/estadisticas";
    public static final String OPINION = "/opinion";
    public static final String OPINION_RESPALDO = OPINION + "/respaldo";
    public static final String AVISOS = "/avisos";
    public static final String AVISOS_CONTADOR = AVISOS + "/contador";

    public static final String ORG_SOLICITUDES = "/org/solicitudes";
    public static final String ORG_SOLICITUD = ORG_SOLICITUDES + "/{id}";
    public static final String ORG_ACEPTAR = ORG_SOLICITUD + "/aceptar";
    public static final String ORG_RECHAZAR = ORG_SOLICITUD + "/rechazar";
    public static final String ORG_COMPLETAR = ORG_SOLICITUD + "/completar";
    public static final String ORG_ARCHIVADAS = ORG_SOLICITUDES + "/archivadas";
    public static final String ORG_INFORME = ORG_SOLICITUDES + "/informe";
    public static final String ORG_PERFIL = "/org/perfil";

    public static final String DEMO_ROL = "/demo/rol";
    public static final String DEMO_ORGANIZACION = "/demo/organizacion";
    public static final String DEMO_REINICIAR = "/demo/reiniciar";

    public static final String DEV_CASOS_DE_USO = "/dev/casos-de-uso";
    public static final String DEV_LAYOUT_SYSTEM = "/dev/layout-system";

    // para Java que arma enlaces (las plantillas usan @{${rutas.X}(id=...)})
    public static String expandir(String plantilla, Object id) {
        return plantilla.replace("{id}", String.valueOf(id));
    }

    private Rutas() {
    }
}
