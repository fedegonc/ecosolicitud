package com.ecosolicitud.demo;

import java.util.Arrays;
import java.util.List;

import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

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

    public DemoService(OrganizacionService organizaciones) {
        this.organizaciones = organizaciones;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (organizaciones.vacia()) {
            reiniciar();
        }
    }

    public void reiniciar() {
        organizaciones.reemplazarTodas(DATASET);
    }
}
