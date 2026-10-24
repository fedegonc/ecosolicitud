package com.ecosolicitud.demo;

import java.util.List;

import com.ecosolicitud.guia.ArticuloSemilla;
import com.ecosolicitud.shared.CatalogoMateriales;

final class DemoGuiaEnvases {

    private DemoGuiaEnvases() {
    }

    static List<ArticuloSemilla> articulos(CatalogoMateriales cat) {
        return List.of(
                new ArticuloSemilla("Plástico", """
                        No todo el plástico es igual. Las botellas de bebida (PET) y los envases
                        rígidos de limpieza o lácteos (PEAD) son los que **mejor se reciclan**:
                        se prensan en fardos, se muelen y vuelven como fibra, envases nuevos
                        o madera plástica.

                        ## Cómo reconocerlo

                        Buscá el triángulo con un número abajo del envase. El **1 (PET)** y el
                        **2 (PEAD)** son los más buscados en los acopios. Los demás números
                        dependen de cada centro: consultá antes de juntarlos.

                        ## Qué se recibe

                        - Botellas de agua, refresco y aceite (enjuagadas)
                        - Bidones y envases de detergente, shampoo o lavandina
                        - Potes rígidos de yogur, manteca o helado, limpios

                        ## Qué no se recibe

                        Film y bolsitas finas, envoltorios metalizados de golosinas, telgopor,
                        juguetes y caños **casi nunca se recuperan**: mezclan varios plásticos
                        o no tienen comprador. Van a la basura común salvo que tu centro diga
                        lo contrario.

                        ## Antes de llevarlo

                        1. Vaciá y enjuagá el envase: el resto de aceite o leche contamina el fardo.
                        2. **Aplastá las botellas** a lo largo: ocupan hasta cinco veces menos.
                        3. Las tapas van aparte, en una bolsa propia.

                        ### Fuentes

                        - [US EPA — Plastics: Material-Specific Data](https://www.epa.gov/facts-and-figures-about-materials-waste-and-recycling/plastics-material-specific-data)
                        - [Ministerio de Ambiente de Uruguay](https://www.gub.uy/ministerio-ambiente)

                        _Foto de portada: [Grendelkhan](https://commons.wikimedia.org/wiki/File:Bales_of_PET_bottles_closeup.jpg), [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0), vía Wikimedia Commons._
                        """, "plastico", cat.resolver("PLASTICO"), 5),
                new ArticuloSemilla("Metal y latas", """
                        El metal se recicla **sin perder calidad**, una y otra vez. Una lata de
                        aluminio reciclada ahorra la mayor parte de la energía que haría falta
                        para fabricarla desde el mineral, por eso es de los materiales que
                        mejor se pagan en los acopios.

                        ## Aluminio o acero

                        Acercale un imán: si **no se pega**, es aluminio (latas de refresco
                        y cerveza); si **se pega**, es acero u hojalata (latas de conserva,
                        tapas de frascos). Separarlos en casa le ahorra trabajo al centro.

                        ## Qué se recibe

                        - Latas de bebida y de conserva, vacías
                        - Tapas metálicas de frascos y botellas
                        - Bandejas y papel de aluminio limpios, hechos una bola
                        - Chatarra chica: ollas viejas, cubiertos, alambre

                        ## Qué no se recibe

                        Garrafas de gas, aerosoles con contenido, latas de pintura o solvente
                        con restos y pilas **no van con el metal**: son residuos peligrosos y
                        tienen su propio circuito.

                        ## Antes de llevarlo

                        1. Enjuagá las latas de conserva para que no junten olor.
                        2. **Aplastá las latas de bebida**; las de conserva podés dejarlas
                           enteras con la tapa adentro.
                        3. Si juntás chatarra grande, avisá al acopio antes de llevarla.

                        ### Fuentes

                        - [US EPA — Aluminum: Material-Specific Data](https://www.epa.gov/facts-and-figures-about-materials-waste-and-recycling/aluminum-material-specific-data)
                        - [US EPA — Ferrous Metals: Material-Specific Data](https://www.epa.gov/facts-and-figures-about-materials-waste-and-recycling/ferrous-metals-material-specific-data)

                        _Foto de portada: [Ruth Hartnup](https://commons.wikimedia.org/wiki/File:Crushed_cans_(9691248780).jpg), [CC BY 2.0](https://creativecommons.org/licenses/by/2.0), vía Wikimedia Commons._
                        """, "metal", cat.resolver("METAL"), 6));
    }
}
