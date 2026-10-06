package com.ecosolicitud;

import java.util.List;

import com.ecosolicitud.demo.DemoService;
import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.CatalogoMateriales;
import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DatasetTest {

    @Autowired
    private OrganizacionService servicio;

    @Autowired
    private DemoService demo;

    @Autowired
    private CatalogoMateriales catalogo;

    @Test
    void datasetSeCargaUnaSolaVez() {
        assertThat(catalogo.activos().stream().map(Material::getCodigo).toList())
                .containsExactly("PLASTICO", "CARTON", "PAPEL", "VIDRIO", "METAL",
                        "ELECTRONICOS");

        var ids = servicio.todas().stream().map(OrganizacionInfo::id).toList();
        assertThat(ids).containsExactlyInAnyOrder(
                "frontera-limpia", "acopio-verde", "coleta-solidaria");
        assertThat(servicio.buscar("coleta-solidaria").orElseThrow().ciudad())
                .isEqualTo(Ciudad.LIVRAMENTO);

        servicio.actualizarPerfil("acopio-verde",
                List.of(new Material("VIDRIO", "Vidrio", "frasco")),
                "Horario alterado", "+598 92 999 888",
                servicio.buscar("acopio-verde").orElseThrow().version());
        demo.run(new DefaultApplicationArguments());

        assertThat(servicio.buscar("acopio-verde").orElseThrow().horario())
                .isEqualTo("Horario alterado");
    }
}
