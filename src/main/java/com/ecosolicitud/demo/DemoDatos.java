package com.ecosolicitud.demo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.ecosolicitud.comunidad.PublicacionSemilla;
import com.ecosolicitud.comunidad.TipoPublicacion;
import com.ecosolicitud.guia.ArticuloSemilla;
import com.ecosolicitud.opinion.OpinionSemilla;
import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.comunidad.RecicladorInfo;
import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.shared.Rol;
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

    static List<OrganizacionInfo> organizaciones() {
        return List.of(
                new OrganizacionInfo("frontera-limpia", "Cooperativa Frontera Limpia",
                        Ciudad.RIVERA,
                        List.of(Material.PLASTICO, Material.CARTON, Material.PAPEL,
                                Material.METAL),
                        "Lun a Vie 8 a 17 h", "+598 92 000 111", 0),
                new OrganizacionInfo("acopio-verde", "Acopio Verde Rivera",
                        Ciudad.RIVERA,
                        List.of(Material.VIDRIO, Material.PLASTICO, Material.CARTON),
                        "Lun a Sáb 9 a 13 h", "+598 92 000 222", 0),
                new OrganizacionInfo("coleta-solidaria", "Coleta Solidária Livramento",
                        Ciudad.LIVRAMENTO, Arrays.asList(Material.values()),
                        "Seg a Sex 8 às 18 h", "+55 55 9000 0000", 0));
    }

    static List<RecicladorInfo> recicladores() {
        return List.of(
                new RecicladorInfo("Luis Pereira", "+598 92 111 222"),
                new RecicladorInfo("María Dos Santos", null),
                new RecicladorInfo("Oscar Ríos", "+598 91 333 444"),
                new RecicladorInfo("José Silva", "+55 55 9111 2222"),
                new RecicladorInfo("Ana Costa", null));
    }

    static List<SolicitudSemilla> solicitudes() {
        var ahora = Instant.now();
        return List.of(
                new SolicitudSemilla("ciudadano-demo", "Martina López", "099 123 456",
                        "Agraciada 1234", "Portón verde",
                        List.of(Material.CARTON, Material.PAPEL),
                        "frontera-limpia", null, Estado.COMPLETADA,
                        ahora.minus(5, ChronoUnit.DAYS),
                        ahora.minus(5, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS),
                        ahora.minus(4, ChronoUnit.DAYS)),
                new SolicitudSemilla("ciudadano-demo", "Martina López", "099 123 456",
                        "Agraciada 1234", null, List.of(Material.PLASTICO),
                        "frontera-limpia", null, Estado.EN_CURSO,
                        ahora.minus(3, ChronoUnit.DAYS),
                        ahora.minus(3, ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS), null),
                new SolicitudSemilla("vecina-ana", "Ana Rodríguez", "098 555 111",
                        "Sarandí 56", "Fondo",
                        List.of(Material.PLASTICO, Material.METAL),
                        "frontera-limpia", null, Estado.PENDIENTE,
                        ahora.minus(2, ChronoUnit.DAYS), null, null),
                new SolicitudSemilla("vecino-bruno", "Bruno Pérez", "099 444 333",
                        "Artigas 890", null, List.of(Material.CARTON),
                        "frontera-limpia", "Dejar en la vereda", Estado.PENDIENTE,
                        ahora.minus(1, ChronoUnit.DAYS), null, null),
                new SolicitudSemilla("vecina-carla", "Carla Santos", "097 222 000",
                        "Ituzaingó 45", null, List.of(Material.VIDRIO),
                        "acopio-verde", null, Estado.RECHAZADA,
                        ahora.minus(6, ChronoUnit.DAYS),
                        ahora.minus(6, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS),
                        ahora.minus(5, ChronoUnit.DAYS)));
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
                        "Cooperativa Frontera Limpia", false,
                        ahora.minus(3, ChronoUnit.DAYS)),
                new AvisoSemilla("ciudadano-demo", idDe(solicitudes,
                        "Martina López", Estado.COMPLETADA), TipoAviso.COMPLETADA,
                        "Cooperativa Frontera Limpia", true,
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
                new PublicacionSemilla("Nuevo horario en Acopio Verde", """
                        Desde este mes el acopio abre también los sábados de 9 a 13.

                        Si tenés vidrio acumulado, es el mejor momento para llevarlo.""",
                        "El acopio de Rivera suma los sábados a su horario.",
                        TipoPublicacion.NOVEDAD, ahora.minus(2, ChronoUnit.DAYS)),
                new PublicacionSemilla("La cooperativa recuperó 3 toneladas", """
                        En lo que va del año, la Cooperativa Frontera Limpia recuperó tres
                        toneladas de cartón y papel: el equivalente a 50 árboles que no se talaron.

                        El mérito es de los vecinos que separan en casa.""",
                        "Frontera Limpia recuperó 3 toneladas de cartón y papel este año.",
                        TipoPublicacion.HISTORIA, ahora.minus(5, ChronoUnit.DAYS)),
                new PublicacionSemilla("Campaña de electrónicos", """
                        Durante octubre se reciben cables, cargadores y celulares viejos
                        sin costo en los tres centros.

                        Los aparatos que todavía funcionan se donan a escuelas.""",
                        "Todo octubre: electrónicos sin costo en los tres centros.",
                        TipoPublicacion.NOVEDAD, ahora.minus(9, ChronoUnit.DAYS)),
                new PublicacionSemilla("Don Luis y su carrito", """
                        Hace doce años que Don Luis pasa por el barrio juntando cartón.

                        Dice que lo que más cambió es la conciencia: "antes había que
                        revolver, ahora la gente ya lo deja separado".""",
                        "Doce años juntando cartón en el barrio, y una sola frase.",
                        TipoPublicacion.HISTORIA, ahora.minus(14, ChronoUnit.DAYS)));
    }

    static List<ArticuloSemilla> guia() {
        return List.of(
                new ArticuloSemilla("Cómo separar en casa", """
                        Separá por material en bolsas o cajas distintas: cuanto menos se mezcle,
                        menos trabajo tiene el acopio y más se recupera.

                        Enjuagá los envases. No hace falta que queden brillantes: alcanza con
                        que no tengan restos de comida, porque eso contamina el resto.

                        Plegá el cartón y las botellas para ocupar menos lugar.""", null, 1),
                new ArticuloSemilla("Vidrio", """
                        El vidrio se recicla infinitas veces sin perder calidad.

                        Se aceptan botellas y frascos. NO se aceptan espejos, vidrio de ventana,
                        cerámicas, focos ni vajilla: tienen composiciones distintas y arruinan el lote.

                        Sacá las tapas y no lo rompas: el vidrio partido lastima a quien lo manipula.""",
                        Material.VIDRIO, 2),
                new ArticuloSemilla("Cartón y papel", """
                        Tiene que estar seco. El cartón mojado o engrasado (una caja de pizza,
                        por ejemplo) no se puede reciclar: va a la basura común.

                        No hace falta sacar la cinta adhesiva ni los ganchos de metal.""",
                        Material.CARTON, 3),
                new ArticuloSemilla("Electrónicos", """
                        Nunca van a la basura común: tienen metales pesados que contaminan el agua.

                        Se reciben cables, cargadores, celulares y electrodomésticos chicos.
                        Si todavía funciona, primero intentá repararlo o donarlo.""",
                        Material.ELECTRONICOS, 4));
    }

    static List<OpinionSemilla> opiniones() {
        var ahora = Instant.now();
        return List.of(
                new OpinionSemilla("NUEVA", 4,
                        "Fácil, pedí el retiro en un minuto.", Rol.CIUDADANO,
                        ahora.minus(3, ChronoUnit.DAYS)),
                new OpinionSemilla("MIS_SOLICITUDES", 5, null, Rol.CIUDADANO,
                        ahora.minus(2, ChronoUnit.DAYS)),
                new OpinionSemilla("ACOPIOS", 3, null, Rol.CIUDADANO,
                        ahora.minus(2, ChronoUnit.DAYS)),
                new OpinionSemilla("ORG_SOLICITUDES", 4, "Clara la bandeja.",
                        Rol.ORGANIZACION, ahora.minus(1, ChronoUnit.DAYS)));
    }
}
