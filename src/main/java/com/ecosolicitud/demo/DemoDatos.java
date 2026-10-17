package com.ecosolicitud.demo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.ecosolicitud.comunidad.PublicacionSemilla;
import com.ecosolicitud.comunidad.TipoPublicacion;
import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.comunidad.RecicladorInfo;
import com.ecosolicitud.shared.CatalogoMateriales;
import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Ubicacion;
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
                        "Lunes a sábado, 7–19 h", "+598 00 000 001",
                        new Ubicacion(-30.9120, -55.5560), 0),
                new OrganizacionInfo("acopio-verde", "Centro de ejemplo – Microcentro",
                        Ciudad.RIVERA,
                        List.of(m(cat, "CARTON"), m(cat, "PAPEL"),
                                m(cat, "PLASTICO"), m(cat, "LATAS")),
                        "Lunes a viernes, 8–17 h", "+598 00 000 002",
                        new Ubicacion(-30.9050, -55.5508), 0),
                new OrganizacionInfo("coleta-solidaria", "Centro de exemplo – Jardins",
                        Ciudad.LIVRAMENTO,
                        List.of(m(cat, "PLASTICO"), m(cat, "CARTON"), m(cat, "PAPEL"),
                                m(cat, "VIDRIO"), m(cat, "METAL"), m(cat, "LATAS")),
                        "Consultar", "+55 00 0000-0003",
                        new Ubicacion(-30.8900, -55.5320), 0));
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

    // Publicaciones reales de la frontera, cada una con su fuente al pie del cuerpo.
    static List<PublicacionSemilla> comunidad() {
        return List.of(
                new PublicacionSemilla("Renacer del Norte: ocho años y 27 familias",
                        "La cooperativa de clasificadores de Rivera cumple ocho años y hoy sostiene a 27 familias.",
                        """
                        Hace ocho años, muchas de las personas que hoy integran la **Cooperativa Renacer del Norte** buscaban materiales entre la basura del vertedero para venderlos o usarlos en sus casas. Hoy son 27 familias con un trabajo formal.

                        El cambio no fue de un día para el otro: llevó capacitación, acompañamiento y, sobre todo, la decisión de organizarse. La cooperativa trabaja en el predio municipal y en el circuito del microcentro de Rivera, donde recoge los reciclables de los comercios con el Sello Rivera Verde.

                        **Cómo podés ayudar:** separá en casa y no mezcles restos de comida con los reciclables. Un material limpio vale más y le facilita el trabajo a quienes lo clasifican.

                        Fuente: [la diaria Rivera](https://ladiaria.com.uy/rivera/articulo/2026/10/renacer-del-norte-la-cooperativa-de-reciclaje-que-cambio-la-vida-de-27-familias-de-rivera/)""",
                        null, "Renacer del Norte", TipoPublicacion.HISTORIA,
                        Instant.parse("2026-10-12T12:00:00Z")),
                new PublicacionSemilla("Contenedores en casa: plan piloto en el barrio Racca",
                        "La Intendencia entrega contenedores gratis en el barrio Racca y evaluará extender el plan a otros barrios.",
                        """
                        La Intendencia de Rivera empezó un **plan piloto de contenedores domiciliarios** en el **barrio Racca**, con entrega gratuita. El objetivo es ordenar la gestión de los residuos y mejorar la limpieza de las calles.

                        El plan lo coordinan la División Higiene y la Oficina de Asuntos Sociales, Comunitarios y de Juventud. La primera etapa fue de visitas casa por casa para explicar cómo funciona el sistema, con ayuda de referentes del barrio. También habrá instancias de educación ambiental.

                        Si los vecinos lo adoptan bien, el plan podría extenderse a **La Virgencita, Bisio y Santa Isabel**.

                        Fuente: [la diaria Rivera](https://ladiaria.com.uy/rivera/articulo/2026/9/plan-piloto-de-contenedores-intradomiciliarios-comienza-a-implementarse-en-barrio-racca/)""",
                        null, "Intendencia de Rivera", TipoPublicacion.NOVEDAD,
                        Instant.parse("2026-09-28T12:00:00Z")),
                new PublicacionSemilla("Nuevo Punto Verde en el Polo de Desarrollo",
                        "La Intendencia anunció la apertura del Punto Verde 2 para el 12 de diciembre de 2025, en el Polo de Desarrollo (ex Abasto Municipal).",
                        """
                        La Intendencia anunció la apertura del Punto Verde 2 para el 12 de diciembre de 2025, en el Polo de Desarrollo (ex Abasto Municipal). La nota indica que recibe residuos para clasificar de lunes a sábado, de 7 a 19 h.

                        Fuente: [Intendencia de Rivera](https://www.rivera.gub.uy/portal/la-intendencia-de-rivera-inaugura-el-nuevo-punto-verde-2-en-el-polo-de-desarrollo/)""",
                        null, "Intendencia de Rivera", TipoPublicacion.NOVEDAD,
                        Instant.parse("2025-12-10T12:00:00Z")),
                new PublicacionSemilla("Plan Vale: más envases recuperados en Rivera y Tranqueras",
                        "Un acuerdo con la industria de envases fortalece las plantas de clasificación y suma ecopuntos.",
                        """
                        La Intendencia de Rivera firmó un convenio con la industria de envases para recuperar más envases en todo el departamento: el **Plan Vale**. Las plantas de clasificación de **Rivera y Tranqueras** quedan en manos de cooperativas de clasificadores, con apoyo técnico de la División Higiene.

                        El plan incluye tres líneas de trabajo:
                        - Controlar la calidad del material que llega a las plantas.
                        - Hacer campañas para que la gente separe mejor en casa.
                        - Ubicar **ecopuntos** donde dejar envases en las localidades del plan.

                        Fuente: [Intendencia de Rivera](https://www.rivera.gub.uy/portal/wp-content/uploads/2025/11/LLAMADO-tdr-EXT.pdf)""",
                        null, "Intendencia de Rivera", TipoPublicacion.NOVEDAD,
                        Instant.parse("2025-11-01T12:00:00Z")),
                new PublicacionSemilla("Rivera recicla desde el microcentro",
                        "54 comercios del centro de Rivera separan sus residuos y tienen el Sello Rivera Verde.",
                        """
                        54 comercios del centro de Rivera separan sus residuos y tienen el Sello Rivera Verde. La Cooperativa Renacer del Norte, con 24 clasificadores, los recolecta. Se recuperan unos 700 kg por día.

                        Fuente: [America Retail & Malls](https://americaretail-malls.com/paises/uruguay/comercios-de-rivera-impulsan-el-reciclaje-con-el-sello-verde/)""",
                        null, "Renacer del Norte", TipoPublicacion.HISTORIA,
                        Instant.parse("2025-03-31T12:00:00Z")),
                new PublicacionSemilla("Novo Horizonte estrena su centro de reciclaje",
                        "La Associação de Catadores Novo Horizonte tiene nueva sede en el barrio Jardins de Livramento, con equipos para separar residuos con seguridad.",
                        """
                        La Associação de Catadores Novo Horizonte tiene nueva sede en el barrio Jardins de Livramento, con equipos para separar residuos con seguridad. Fue parte del proyecto binacional Fronteira da Paz Sustentável.

                        Fuente: [Prefeitura de Sant'Ana do Livramento](https://www.santanadolivramento.rs.gov.br/noticia/impressao?id=8560)""",
                        null, "Novo Horizonte", TipoPublicacion.HISTORIA,
                        Instant.parse("2024-11-13T12:00:00Z")));
    }
}
