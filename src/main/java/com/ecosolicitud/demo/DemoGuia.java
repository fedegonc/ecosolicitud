package com.ecosolicitud.demo;

import java.util.List;

import com.ecosolicitud.guia.ArticuloSemilla;
import com.ecosolicitud.shared.CatalogoMateriales;
import com.ecosolicitud.shared.Material;

// Los artículos de la guía son markdown: el cuerpo se renderiza a HTML
// en el servicio. Cada artículo referencia su foto por slug
// (static/img/guia/{slug}-800.webp y -1600.webp) y cita sus fuentes.
final class DemoGuia {

    private DemoGuia() {
    }

    private static Material m(CatalogoMateriales cat, String codigo) {
        return cat.resolver(codigo);
    }

    static List<ArticuloSemilla> guia(CatalogoMateriales cat) {
        var articulos = new java.util.ArrayList<>(List.of(
                new ArticuloSemilla("Cómo separar en casa", """
                        Separar bien en casa es la mitad del trabajo. Un envase limpio y en la
                        bolsa correcta llega al acopio listo para reciclarse; uno sucio o mezclado
                        puede arruinar un lote entero.

                        ## La regla básica: seco y limpio

                        Los materiales tienen que ir **secos y sin restos de comida**. No hace
                        falta lavarlos a fondo: un enjuague rápido alcanza. Una botella con aceite
                        o una caja de pizza grasosa contamina el papel y el cartón que las rodean,
                        y ese material deja de recuperarse.

                        ## Cómo organizarlo

                        - **Un recipiente por material**: papel y cartón en uno, envases
                          plásticos y latas en otro, vidrio aparte.
                        - **Aplastá y plegá**: botellas y cajas ocupan mucho menos si viajan
                          prensadas.
                        - **Sacá las tapas**: las de botellas y frascos son de otro material
                          y se procesan aparte.
                        - **Nada de bolsa negra**: lo reciclable viaja suelto o en bolsa
                          transparente, porque en el acopio tienen que ver qué lleva adentro.

                        ## Qué va a la basura común

                        Film transparente, papel aluminio sucio, vajilla rota, pañales y papel
                        engomado **no se reciclan** en los acopios locales. Ante la duda,
                        consultá la guía o preguntale a tu centro antes de mezclarlos.

                        ### Fuentes

                        - [US EPA — Recycling Basics](https://www.epa.gov/recycle)
                        - [Ministerio de Ambiente de Uruguay](https://www.gub.uy/ministerio-ambiente)

                        _Foto de portada: [E961](https://commons.wikimedia.org/wiki/File:Sorted_waste_containers_close-up.jpg), [CC BY 4.0](https://creativecommons.org/licenses/by/4.0), vía Wikimedia Commons._
                        """, "separar", null, 1),
                new ArticuloSemilla("Vidrio", """
                        El vidrio es el material más noble del reciclaje: **se funde y se rehace
                        infinitas veces** sin perder calidad ni pureza. Una botella reciclada
                        puede volver a la góndola en pocas semanas.

                        ## Qué se recibe

                        - Botellas y frascos de vidrio de cualquier color
                        - Envases limpios y, en lo posible, enteros

                        ## Qué no se recibe

                        Espejos, vidrio de ventana, loza, cerámica, focos y cristal templado
                        **no son vidrio de envase**: se funden a otra temperatura y una sola
                        pieza puede arruinar todo el lote. Esos van a la basura común o a
                        puntos especiales.

                        ## Antes de llevarlo

                        1. Enjuagá el envase y sacale la tapa o el corcho.
                        2. No hace falta quitar las etiquetas de papel.
                        3. No lo rompas "para que entre más": el vidrio partido corta a quienes
                           lo manipulan.

                        ### Fuentes

                        - [Glass Packaging Institute](https://www.gpi.org)
                        - [US EPA — Glass: Material-Specific Data](https://www.epa.gov/facts-and-figures-about-materials-waste-and-recycling/glass-material-specific-data)

                        _Foto de portada: [Donald Trung Quoc Don](https://commons.wikimedia.org/wiki/File:Glass_bottles_next_to_glass_recycling_containers,_Hillegersberg,_Rotterdam_(2021)_02.jpg), [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0), vía Wikimedia Commons._
                        """, "vidrio", m(cat, "VIDRIO"), 2),
                new ArticuloSemilla("Cartón y papel", """
                        Las fibras de papel y cartón se reciclan entre **cinco y siete veces**
                        antes de acortarse demasiado para reutilizarse. Cada tonelada recuperada
                        evita talar árboles y ahorra agua y energía en la fabricación.

                        ## La condición: que esté seco

                        La fibra mojada o grasosa no se recupera. Por eso:

                        - **Sí**: cajas plegadas, diarios, hojas, cuadernos sin espiral, sobres
                          y cartulina.
                        - **No**: caja de pizza con grasa, papel toalla usado, papel encerado
                          o plastificado, fotos.

                        ## Cómo prepararlo

                        Plegá las cajas para que ocupen menos lugar. No hace falta sacar ganchos
                        metálicos ni cinta adhesiva: la planta los separa en el proceso.
                        Los tetra brik **no van con el cartón**: son un envase multicapa
                        (cartón, plástico y aluminio) que necesita otro tratamiento.

                        ### Fuentes

                        - [American Forest & Paper Association — Recycling](https://www.afandpa.org)
                        - [US EPA — Paper and Paperboard: Material-Specific Data](https://www.epa.gov/facts-and-figures-about-materials-waste-and-recycling/paper-and-paperboard-material-specific-data)

                        _Foto de portada: [Walmart](https://commons.wikimedia.org/wiki/File:Box_baler_at_Walmart.jpg), [CC BY 2.0](https://creativecommons.org/licenses/by/2.0), vía Wikimedia Commons._
                        """, "carton", m(cat, "CARTON"), 3),
                new ArticuloSemilla("Electrónicos", """
                        No hay un servicio de recepción de electrónicos verificado en esta demo.
                        Consultá a las autoridades locales antes de llevar estos residuos.

                        Los residuos electrónicos son los que más rápido crecen en el mundo:
                        el *Global E-waste Monitor* estima más de **60 millones de toneladas
                        por año** y apenas una quinta parte se recicla de forma documentada.
                        Adentro hay cobre, oro y aluminio recuperables, pero también plomo,
                        mercurio y cadmio que contaminan suelo y agua si terminan en un volcadero.

                        ## Qué llevar

                        Cables, cargadores, celulares, pilas, auriculares y electrodomésticos
                        chicos. En general, todo lo que tenga enchufe o batería entra en
                        la categoría.

                        ## Antes de entregarlo

                        1. **Borrá tus datos**: restablecé el celular de fábrica y sacale
                           el chip y la tarjeta de memoria.
                        2. Si todavía funciona, considerá **repararlo o donarlo**: reutilizar
                           siempre es mejor que reciclar.
                        3. Las pilas sueltas van aparte; si una batería está hinchada, avisá
                           al acopio antes de llevarla.

                        ## Por qué no van a la basura común

                        Un solo aparato en el relleno libera metales pesados durante décadas.
                        La Convención de Basilea regula su traslado entre países justamente
                        porque son residuos peligrosos.

                        ### Fuentes

                        - [Global E-waste Monitor — ITU/UNITAR](https://ewastemonitor.info)
                        - [Convención de Basilea](https://www.basel.int)

                        _Foto de portada: [Syced](https://commons.wikimedia.org/wiki/File:Electronic_junk_separation_in_view_of_recycling.jpg), [CC0](http://creativecommons.org/publicdomain/zero/1.0/deed.en), vía Wikimedia Commons._
                        """, "electronicos", m(cat, "ELECTRONICOS"), 4),
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
                        """, "plastico", m(cat, "PLASTICO"), 5),
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
                        """, "metal", m(cat, "METAL"), 6)));
        articulos.addAll(DemoGuiaSinRetiro.articulos());
        return articulos;
    }
}
