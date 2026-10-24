package com.ecosolicitud;

import java.util.List;

import com.ecosolicitud.demo.DemoService;
import com.ecosolicitud.comunidad.ComunidadService;
import com.ecosolicitud.guia.ArticuloInfo;
import com.ecosolicitud.guia.GuiaService;
import com.ecosolicitud.solicitud.Estado;
import com.ecosolicitud.solicitud.SolicitudService;
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
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@ActiveProfiles("test")
class DatasetTest {

    @Autowired
    private OrganizacionService servicio;

    @Autowired
    private DemoService demo;

    @Autowired
    private CatalogoMateriales catalogo;

    @Autowired
    private SolicitudService solicitudes;

    @Autowired
    private ComunidadService comunidad;

    @Autowired
    private GuiaService guia;

    @Test
    void guiaConservaLosArticulosYElOrdenDelDataset() {
        demo.reiniciar();
        assertThat(guia.articulos()).extracting(ArticuloInfo::titulo, ArticuloInfo::orden)
                .containsExactly(
                        tuple("Cómo separar en casa", 1),
                        tuple("Vidrio", 2),
                        tuple("Cartón y papel", 3),
                        tuple("Electrónicos", 4),
                        tuple("Plástico", 5),
                        tuple("Metal y latas", 6),
                        tuple("Escombros", 7),
                        tuple("Orgánicos y restos de poda", 8),
                        tuple("Muebles y voluminosos", 9));
        assertThat(guia.articulos().subList(4, 6))
                .extracting(ArticuloInfo::imagen, a -> a.material().getCodigo())
                .containsExactly(tuple("plastico", "PLASTICO"), tuple("metal", "METAL"));
    }

    @Test
    void datasetDeEjemploBinacional() {
        demo.reiniciar();
        String paso = "Centro de ejemplo – Paso del Enano";
        String microcentro = "Centro de ejemplo – Microcentro";
        String jardins = "Centro de exemplo – Jardins";
        assertThat(servicio.todas()).extracting(OrganizacionInfo::nombre, OrganizacionInfo::ciudad,
                        OrganizacionInfo::horario, OrganizacionInfo::telefono,
                        o -> o.materiales().stream().map(Material::getCodigo).toList())
                .containsExactlyInAnyOrder(
                        tuple(paso, Ciudad.RIVERA, "Lunes a sábado, 7–19 h", "+598 00 000 001",
                                List.of("PLASTICO", "CARTON", "PAPEL", "VIDRIO", "METAL", "LATAS")),
                        tuple(microcentro, Ciudad.RIVERA, "Lunes a viernes, 8–17 h", "+598 00 000 002",
                                List.of("CARTON", "PAPEL", "PLASTICO", "LATAS")),
                        tuple(jardins, Ciudad.LIVRAMENTO, "Consultar", "+55 00 0000-0003",
                                List.of("PLASTICO", "CARTON", "PAPEL", "VIDRIO", "METAL", "LATAS")));
        assertThat(servicio.todas()).allSatisfy(o -> {
            assertThat(o.ubicacion()).isNotNull();
            assertThat(o.materiales()).extracting(Material::getCodigo).doesNotContain("ELECTRONICOS");
        });
        assertThat(solicitudes.todas()).hasSize(6).allSatisfy(s -> {
            assertThat(s.ubicacion()).isNull();
            assertThat(s.referencia()).isNull();
            assertThat(s.contactoEsTelefono()).isTrue();
        });
        assertThat(solicitudes.todas()).extracting(s -> s.nombreCiudadano(), s -> s.contacto(),
                        s -> s.direccion(), s -> s.organizacionNombre(), s -> s.estado(), s -> s.nota(),
                        s -> s.materiales().stream().map(Material::getCodigo).toList())
                .containsExactlyInAnyOrder(
                        tuple("Martina López", "099 000 001", "Centro de Rivera, cerca de Plaza Internacional",
                                paso, Estado.COMPLETADA, "Portón verde", List.of("CARTON", "PAPEL")),
                        tuple("Martina López", "099 000 001", "Centro de Rivera",
                                paso, Estado.EN_CURSO, null, List.of("PLASTICO")),
                        tuple("Ana Rodríguez", "099 000 002", "Rivera, zona centro",
                                paso, Estado.PENDIENTE, "Fondo", List.of("PLASTICO", "METAL")),
                        tuple("Bruno Pérez", "099 000 003", "Rivera",
                                microcentro, Estado.PENDIENTE, "Dejar en la vereda", List.of("CARTON")),
                        tuple("Carla Santos", "(55) 90000-0004", "Bairro Parque do Sol, Livramento",
                                jardins, Estado.RECHAZADA, "Fora do horário", List.of("VIDRIO")),
                        tuple("João Almeida", "(55) 90000-0005", "Bairro Cohab do Armour, Livramento",
                                jardins, Estado.PENDIENTE, "Deixar no portão", List.of("PLASTICO", "LATAS")));
        assertThat(comunidad.recicladores()).extracting(r -> r.nombre(), r -> r.ciudad(), r -> r.telefono())
                .containsExactlyInAnyOrder(
                        tuple("Luis Pereira", Ciudad.RIVERA, "+598 00 000 011"),
                        tuple("María Dos Santos", Ciudad.LIVRAMENTO, null),
                        tuple("Oscar Ríos", Ciudad.RIVERA, "+598 00 000 012"),
                        tuple("José Silva", Ciudad.LIVRAMENTO, "+55 00 0000-0013"),
                        tuple("Ana Costa", Ciudad.RIVERA, null));
        catalogo.semillarFaltantes();
        assertThat(catalogo.todos()).hasSize(7);
    }

    @Test
    void datasetSeCargaUnaSolaVez() {
        assertThat(catalogo.activos().stream().map(Material::getCodigo).toList())
                .containsExactly("PLASTICO", "CARTON", "PAPEL", "VIDRIO", "METAL",
                        "ELECTRONICOS", "LATAS");

        var ids = servicio.todas().stream().map(OrganizacionInfo::id).toList();
        assertThat(ids).containsExactlyInAnyOrder(
                "frontera-limpia", "acopio-verde", "coleta-solidaria");
        assertThat(servicio.buscar("coleta-solidaria").orElseThrow().ciudad())
                .isEqualTo(Ciudad.LIVRAMENTO);

        servicio.actualizarPerfil("acopio-verde",
                List.of(new Material("VIDRIO", "Vidrio", "frasco")),
                "Horario alterado", "+598 92 999 888", null,
                servicio.buscar("acopio-verde").orElseThrow().version());
        demo.run(new DefaultApplicationArguments());

        assertThat(servicio.buscar("acopio-verde").orElseThrow().horario())
                .isEqualTo("Horario alterado");
    }
}
