package com.ecosolicitud.demo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import com.ecosolicitud.comunidad.PublicacionSemilla;
import com.ecosolicitud.comunidad.TipoPublicacion;
import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.comunidad.RecicladorInfo;
import com.ecosolicitud.shared.CatalogoMateriales;
import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.solicitud.AvisoSemilla;
import com.ecosolicitud.solicitud.Estado;
import com.ecosolicitud.solicitud.SolicitudInfo;
import com.ecosolicitud.solicitud.SolicitudSemilla;
import com.ecosolicitud.solicitud.TipoAviso;

// El contenido de demostración vive acá: se edita directo en el servidor
// hasta que el volumen de usuarios justifique tablas propias.
final class DemoDatos {

    private DemoDatos() {
    }

    static List<OrganizacionInfo> organizaciones(CatalogoMateriales cat) {
        return List.of(
                new OrganizacionInfo("frontera-limpia", "Centro de ejemplo – Paso del Enano",
                        Ciudad.RIVERA,
                        List.of(m(cat, "PLASTICO"), m(cat, "CARTON"), m(cat, "PAPEL"),
                                m(cat, "VIDRIO"), m(cat, "METAL"), m(cat, "LATAS")),
                        "Lunes a sábado, 7–19 h", "+598 00 000 001", null, 0),
                new OrganizacionInfo("acopio-verde", "Centro de ejemplo – Microcentro",
                        Ciudad.RIVERA,
                        List.of(m(cat, "CARTON"), m(cat, "PAPEL"),
                                m(cat, "PLASTICO"), m(cat, "LATAS")),
                        "Lunes a viernes, 8–17 h", "+598 00 000 002", null, 0),
                new OrganizacionInfo("coleta-solidaria", "Centro de exemplo – Jardins",
                        Ciudad.LIVRAMENTO,
                        List.of(m(cat, "PLASTICO"), m(cat, "CARTON"), m(cat, "PAPEL"),
                                m(cat, "VIDRIO"), m(cat, "METAL"), m(cat, "LATAS")),
                        "Consultar", "+55 00 0000-0003", null, 0));
    }

    private static Material m(CatalogoMateriales cat, String codigo) {
        return cat.resolver(codigo);
    }

    static List<RecicladorInfo> recicladores() {
        return List.of(
                new RecicladorInfo("Luis Pereira", Ciudad.RIVERA, "+598 00 000 011"),
                new RecicladorInfo("María Dos Santos", Ciudad.LIVRAMENTO, null),
                new RecicladorInfo("Oscar Ríos", Ciudad.RIVERA, "+598 00 000 012"),
                new RecicladorInfo("José Silva", Ciudad.LIVRAMENTO, "+55 00 0000-0013"),
                new RecicladorInfo("Ana Costa", Ciudad.RIVERA, null));
    }

    static List<SolicitudSemilla> solicitudes(CatalogoMateriales cat) {
        var ahora = Instant.now();
        return List.of(
                new SolicitudSemilla("ciudadano-demo", "Martina López", "099 000 001",
                        "Centro de Rivera, cerca de Plaza Internacional", null, null,
                        List.of(m(cat, "CARTON"), m(cat, "PAPEL")),
                        "frontera-limpia", "Portón verde", Estado.COMPLETADA,
                        ahora.minus(5, ChronoUnit.DAYS),
                        ahora.minus(5, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS),
                        ahora.minus(4, ChronoUnit.DAYS)),
                new SolicitudSemilla("ciudadano-demo", "Martina López", "099 000 001",
                        "Centro de Rivera", null, null,
                        List.of(m(cat, "PLASTICO")),
                        "frontera-limpia", null, Estado.EN_CURSO,
                        ahora.minus(3, ChronoUnit.DAYS),
                        ahora.minus(3, ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS), null),
                new SolicitudSemilla("vecina-ana", "Ana Rodríguez", "099 000 002",
                        "Rivera, zona centro", null, null,
                        List.of(m(cat, "PLASTICO"), m(cat, "METAL")),
                        "frontera-limpia", "Fondo", Estado.PENDIENTE,
                        ahora.minus(2, ChronoUnit.DAYS), null, null),
                new SolicitudSemilla("vecino-bruno", "Bruno Pérez", "099 000 003",
                        "Rivera", null, null,
                        List.of(m(cat, "CARTON")),
                        "acopio-verde", "Dejar en la vereda", Estado.PENDIENTE,
                        ahora.minus(1, ChronoUnit.DAYS), null, null),
                new SolicitudSemilla("vecina-carla", "Carla Santos", "(55) 90000-0004",
                        "Bairro Parque do Sol, Livramento", null, null,
                        List.of(m(cat, "VIDRIO")),
                        "coleta-solidaria", "Fora do horário", Estado.RECHAZADA,
                        ahora.minus(6, ChronoUnit.DAYS),
                        // rechazo directo: respuesta y cierre en el mismo instante
                        ahora.minus(6, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS),
                        ahora.minus(6, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS)),
                new SolicitudSemilla("vecino-joao", "João Almeida", "(55) 90000-0005",
                        "Bairro Cohab do Armour, Livramento", null, null,
                        List.of(m(cat, "PLASTICO"), m(cat, "LATAS")),
                        "coleta-solidaria", "Deixar no portão", Estado.PENDIENTE,
                        ahora.minus(1, ChronoUnit.DAYS), null, null));
    }

    // los avisos semilla se atan a solicitudes reales para mostrar su #id
    static List<AvisoSemilla> avisos(List<SolicitudInfo> solicitudes) {
        var ahora = Instant.now();
        return List.of(
                new AvisoSemilla("frontera-limpia", idDe(solicitudes,
                        "Ana Rodríguez", Estado.PENDIENTE), TipoAviso.NUEVA,
                        "Ana Rodríguez", false, ahora.minus(2, ChronoUnit.DAYS)),
                new AvisoSemilla("ciudadano-demo", idDe(solicitudes,
                        "Martina López", Estado.EN_CURSO), TipoAviso.ACEPTADA,
                        "Centro de ejemplo – Paso del Enano", false,
                        ahora.minus(3, ChronoUnit.DAYS)),
                new AvisoSemilla("ciudadano-demo", idDe(solicitudes,
                        "Martina López", Estado.COMPLETADA), TipoAviso.COMPLETADA,
                        "Centro de ejemplo – Paso del Enano", true,
                        ahora.minus(4, ChronoUnit.DAYS)));
    }

    private static Long idDe(List<SolicitudInfo> solicitudes, String nombre,
            Estado estado) {
        return solicitudes.stream()
                .filter(s -> s.nombreCiudadano().equals(nombre)
                        && s.estado() == estado)
                .map(SolicitudInfo::id).findFirst().orElse(null);
    }

    static List<PublicacionSemilla> comunidad() {
        var ahora = Instant.now();
        return List.of(
                new PublicacionSemilla("Centros de ejemplo para probar", """
                        Los centros de esta demostración sirven para probar solicitudes.
                        Sus teléfonos son ficticios. No representan organizaciones participantes
                        ni servicios de retiro confirmados.

                        Usá datos de prueba y no coordines un retiro real con estos contactos.""",
                        "Esta demostración usa centros y contactos de ejemplo.",
                        "acopio", "EcoSolicitud", TipoPublicacion.NOVEDAD, ahora.minus(2, ChronoUnit.DAYS)),
                new PublicacionSemilla("Separá tus materiales", """
                        Separá cartón, papel, plástico, vidrio, metal y latas.
                        Revisá los materiales que acepta el centro antes de enviar la solicitud.

                        Los centros de ejemplo no reciben electrónicos.""",
                        "Revisá los materiales del centro antes de pedir el retiro.",
                        "cooperativa", "EcoSolicitud", TipoPublicacion.HISTORIA, ahora.minus(5, ChronoUnit.DAYS)),
                new PublicacionSemilla("Cómo probar una solicitud", """
                        Elegí tu ciudad, un centro de ejemplo y los materiales.
                        Escribí un contacto y una dirección ficticios.

                        Luego podés cambiar al perfil Organización para probar la atención del pedido.""",
                        "Probá el recorrido con datos ficticios, sin señalar una casa real.",
                        "campana", "EcoSolicitud", TipoPublicacion.NOVEDAD, ahora.minus(9, ChronoUnit.DAYS)),
                new PublicacionSemilla("Recolectores de demostración", """
                        Las personas de esta lista son ejemplos para la demostración.
                        Algunas no tienen teléfono cargado.

                        Los contactos reales se incorporarán en otra fase, con su autorización.""",
                        "La lista incluye contactos de prueba de Rivera y Livramento.",
                        "carrito", "EcoSolicitud", TipoPublicacion.HISTORIA, ahora.minus(14, ChronoUnit.DAYS)));
    }
}
