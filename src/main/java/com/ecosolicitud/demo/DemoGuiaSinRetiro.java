package com.ecosolicitud.demo;

import java.util.List;

import com.ecosolicitud.guia.ArticuloSemilla;

// Artículos de la guía para residuos que NADIE retira en la demo: no están en
// el catálogo de materiales porque ningún centro los acepta como pedido.
// Acá se explica el circuito real (municipal o "llevalo vos") con fuentes.
final class DemoGuiaSinRetiro {

    private DemoGuiaSinRetiro() {
    }

    static List<ArticuloSemilla> articulos() {
        return List.of(
                new ArticuloSemilla("Escombros", """
                        Los escombros **no se reciclan en los acopios ni los levanta el camión
                        de la basura**: la Intendencia de Rivera aclara que no recolecta tierra
                        ni escombros. Son restos de obra y su destino es llevarlos vos.

                        ## Qué cuenta como escombro

                        Hormigón, ladrillos, bloques, cerámica, restos de mezcla, azulejos,
                        sanitarios rotos y tierra de excavación. Si quedó de una obra,
                        reforma o demolición, es escombro — no va ni a la bolsa de reciclables
                        ni al contenedor común.

                        ## En Rivera

                        El **Punto Verde de la Cantera Municipal** recibe desechos de obras
                        y escombros de lunes a sábados, de 7:30 a 19 h. Desde ahí se trasladan
                        al Vertedero Municipal de Paso del Enano. Para obras grandes, la
                        Ley 19.829 y el Decreto 213/025 regulan los residuos de construcción:
                        corresponde gestionarlos con volquetes o gestores habilitados, no
                        sacarlos a la vereda.

                        ## En Livramento

                        El entulho se gestiona por la Prefeitura. Antes de sacarlo a la calle,
                        consultá a la Secretaría Municipal de Serviços Urbanos
                        (tel. 3968-1124) por los puntos habilitados.

                        Tirar escombros en baldíos, veredas o zanjas es un basural informal:
                        tapa desagües, junta agua estancada y es causal de multa.

                        ### Fuentes

                        - [IDR — Punto Verde, Cantera Municipal](https://www.rivera.gub.uy/portal/inicia-el-lunes-9-de-junio-el-proyecto-punto-verde/)
                        - [IDR — Residuos sólidos urbanos](https://www.rivera.gub.uy/medioambiente/estrategias/residuos/)
                        - [IMPO — Decreto 213/025, residuos de construcción](https://impo.com.uy/bases/decretos/213-2025/1)
                        - [Prefeitura de Livramento — Preguntas frecuentes](http://sdolivramento.com.br/faq)
                        """, null, null, 5),
                new ArticuloSemilla("Orgánicos y restos de poda", """
                        Restos de comida y recortes de jardín son buena parte del residuo
                        domiciliario. Los acopios de esta demo no los retiran: van al
                        contenedor común, al programa municipal de recolección o, mejor
                        todavía, al compost.

                        ## Compostar en casa

                        Con un cajón o una compostera en el patio se transforman en abono:
                        alterná **húmedos** (cáscaras, restos de fruta y verdura, yerba,
                        borra de café) con **secos** (hojas, cartón sin tinta, aserrín),
                        revolvé cada tanto y listo. Evitá carnes, lácteos y grasas, que
                        atraen plagas.

                        ## Poda en Rivera

                        La Intendencia hace **recolección de residuos extraordinarios por
                        calendario** en 15 zonas de la ciudad (una vez al mes por zona; los
                        sábados en la más céntrica). Los vegetales se dejan en montículos
                        separados del resto y se destinan a compost. El día que pasa por
                        tu zona figura en el calendario imantado que se entrega cada año.
                        También podés llevar los restos de poda al Punto Verde de la
                        Cantera Municipal (lun a sáb, 7:30 a 19 h).

                        ## Poda en Livramento

                        La Prefeitura tiene una **Central de Recebimento de Resíduos de
                        Poda** para entregar los recortes. Si el árbol está en la vereda
                        o la vía pública, la poda se solicita a la Secretaría Municipal
                        de Serviços Urbanos (tel. 3968-1124); cortarla por cuenta propia
                        sin autorización puede ser denunciado.

                        ### Fuentes

                        - [IDR — Residuos sólidos urbanos](https://www.rivera.gub.uy/medioambiente/estrategias/residuos/)
                        - [IDR — Punto Verde, Cantera Municipal](https://www.rivera.gub.uy/portal/inicia-el-lunes-9-de-junio-el-proyecto-punto-verde/)
                        - [Prefeitura de Livramento — Central de Resíduos de Poda](http://www.sdolivramento.com.br/documento/view/167/prefeitura-municipal-central-de-recebimentos-residuos-de-poda-2023-2025)
                        - [US EPA — Composting at Home](https://www.epa.gov/recycle)
                        """, null, null, 6),
                new ArticuloSemilla("Muebles y voluminosos", """
                        Una heladera vieja o un colchón no entran en el contenedor y tampoco
                        los retiran los acopios. Son **residuos extraordinarios**: tienen
                        su propio circuito, que no pasa por esta app.

                        ## Qué entra en la categoría

                        Muebles, colchones, chatarras grandes y electrodomésticos que ya
                        no sirven — si funcionan, **donarlos o venderlos siempre es la
                        primera opción**: una segunda vida vale más que cualquier reciclaje.

                        ## En Rivera: recolección por calendario

                        La Intendencia levanta voluminosos (muebles en desuso, chatarras,
                        podas) en **15 zonas, una vez al mes por zona** — la más céntrica,
                        los sábados. El calendario imantado que llega cada año a cada
                        domicilio indica el día que pasa por tu zona. Regla de oro:
                        **sacarlo el día que toca, no antes** — un mueble días en la vereda
                        se convierte en basural improvisado. También se pueden llevar al
                        Vertedero de Paso del Enano o a las volquetas habilitadas en la ciudad.

                        ## En Livramento

                        La coleta de móveis y volumosos corre por la Prefeitura; consultá
                        los días y puntos con la Secretaría Municipal de Serviços Urbanos
                        (tel. 3968-1124).

                        ### Fuentes

                        - [IDR — Residuos sólidos urbanos](https://www.rivera.gub.uy/medioambiente/estrategias/residuos/)
                        - [Prefeitura de Livramento — Preguntas frecuentes](http://sdolivramento.com.br/faq)
                        """, null, null, 7));
    }
}
