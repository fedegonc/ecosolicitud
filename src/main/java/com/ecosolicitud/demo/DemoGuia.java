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
        return List.of(
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
                        """, "electronicos", m(cat, "ELECTRONICOS"), 4));
    }
}
