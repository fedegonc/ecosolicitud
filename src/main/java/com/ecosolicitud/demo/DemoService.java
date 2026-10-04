package com.ecosolicitud.demo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import com.ecosolicitud.comunidad.ComunidadService;
import com.ecosolicitud.comunidad.PublicacionSemilla;
import com.ecosolicitud.comunidad.TipoPublicacion;
import com.ecosolicitud.guia.ArticuloSemilla;
import com.ecosolicitud.guia.GuiaService;
import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.organizacion.RecuperadorInfo;
import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.solicitud.Estado;
import com.ecosolicitud.solicitud.SolicitudSemilla;
import com.ecosolicitud.solicitud.SolicitudService;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoService implements ApplicationRunner {

    private static final List<OrganizacionInfo> DATASET = List.of(
            new OrganizacionInfo("frontera-limpia", "Cooperativa Frontera Limpia",
                    Ciudad.RIVERA,
                    List.of(Material.PLASTICO, Material.CARTON, Material.PAPEL, Material.METAL),
                    "Lun a Vie 8 a 17 h", "+598 92 000 111", 0),
            new OrganizacionInfo("acopio-verde", "Acopio Verde Rivera",
                    Ciudad.RIVERA,
                    List.of(Material.VIDRIO, Material.PLASTICO, Material.CARTON),
                    "Lun a Sáb 9 a 13 h", "+598 92 000 222", 0),
            new OrganizacionInfo("coleta-solidaria", "Coleta Solidária Livramento",
                    Ciudad.LIVRAMENTO,
                    Arrays.asList(Material.values()),
                    "Seg a Sex 8 às 18 h", "+55 55 9000 0000", 0));

    private final OrganizacionService organizaciones;
    private final SolicitudService solicitudes;
    private final GuiaService guia;
    private final ComunidadService comunidad;

    public DemoService(OrganizacionService organizaciones,
            SolicitudService solicitudes, GuiaService guia,
            ComunidadService comunidad) {
        this.organizaciones = organizaciones;
        this.solicitudes = solicitudes;
        this.guia = guia;
        this.comunidad = comunidad;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (organizaciones.vacia()) {
            reiniciar();
        }
    }

    @Transactional
    public void reiniciar() {
        organizaciones.reemplazarTodas(DATASET);
        solicitudes.reemplazarTodas(datasetSolicitudes());
        guia.reemplazarTodos(datasetGuia());
        comunidad.reemplazarTodas(datasetComunidad());
        organizaciones.reemplazarEquipo("frontera-limpia", List.of(
                new RecuperadorInfo("Luis Pereira", "+598 92 111 222"),
                new RecuperadorInfo("María Dos Santos", null)));
        organizaciones.reemplazarEquipo("acopio-verde", List.of(
                new RecuperadorInfo("Oscar Ríos", "+598 91 333 444")));
        organizaciones.reemplazarEquipo("coleta-solidaria", List.of(
                new RecuperadorInfo("José Silva", "+55 55 9111 2222"),
                new RecuperadorInfo("Ana Costa", null)));
    }

    private List<PublicacionSemilla> datasetComunidad() {
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

                        Dice que lo que más cambió es la conciencia: \"antes había que
                        revolver, ahora la gente ya lo deja separado\".""",
                        "Doce años juntando cartón en el barrio, y una sola frase.",
                        TipoPublicacion.HISTORIA, ahora.minus(14, ChronoUnit.DAYS)));
    }

    private List<ArticuloSemilla> datasetGuia() {
        return List.of(
                new ArticuloSemilla("C\u00f3mo separar en casa", """
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
                new ArticuloSemilla("Cart\u00f3n y papel", """
                        Tiene que estar seco. El cartón mojado o engrasado (una caja de pizza,
                        por ejemplo) no se puede reciclar: va a la basura común.

                        No hace falta sacar la cinta adhesiva ni los ganchos de metal.""",
                        Material.CARTON, 3),
                new ArticuloSemilla("Electr\u00f3nicos", """
                        Nunca van a la basura común: tienen metales pesados que contaminan el agua.

                        Se reciben cables, cargadores, celulares y electrodomésticos chicos.
                        Si todavía funciona, primero intentá repararlo o donarlo.""",
                        Material.ELECTRONICOS, 4));
    }

    private List<SolicitudSemilla> datasetSolicitudes() {
        var ahora = Instant.now();
        return List.of(
                new SolicitudSemilla("ciudadano-demo", "Ciudadano demo",
                        "Agraciada 1234", "Portón verde",
                        List.of(Material.CARTON, Material.PAPEL),
                        "frontera-limpia", null, Estado.COMPLETADA,
                        ahora.minus(5, ChronoUnit.DAYS), ahora.minus(4, ChronoUnit.DAYS)),
                new SolicitudSemilla("ciudadano-demo", "Ciudadano demo",
                        "Agraciada 1234", null, List.of(Material.PLASTICO),
                        "frontera-limpia", null, Estado.EN_CURSO,
                        ahora.minus(3, ChronoUnit.DAYS), null),
                new SolicitudSemilla("vecina-ana", "Ana Rodríguez",
                        "Sarandí 56", "Fondo",
                        List.of(Material.PLASTICO, Material.METAL),
                        "frontera-limpia", null, Estado.PENDIENTE,
                        ahora.minus(2, ChronoUnit.DAYS), null),
                new SolicitudSemilla("vecino-bruno", "Bruno Pérez",
                        "Artigas 890", null, List.of(Material.CARTON),
                        "frontera-limpia", "Dejar en la vereda", Estado.PENDIENTE,
                        ahora.minus(1, ChronoUnit.DAYS), null),
                new SolicitudSemilla("vecina-carla", "Carla Santos",
                        "Ituzaingó 45", null, List.of(Material.VIDRIO),
                        "acopio-verde", null, Estado.RECHAZADA,
                        ahora.minus(6, ChronoUnit.DAYS), ahora.minus(5, ChronoUnit.DAYS)));
    }
}
