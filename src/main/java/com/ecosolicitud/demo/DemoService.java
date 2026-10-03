package com.ecosolicitud.demo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.organizacion.OrganizacionService;
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

    public DemoService(OrganizacionService organizaciones,
            SolicitudService solicitudes) {
        this.organizaciones = organizaciones;
        this.solicitudes = solicitudes;
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
