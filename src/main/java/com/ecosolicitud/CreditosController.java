package com.ecosolicitud;

import java.util.List;

import com.ecosolicitud.shared.Miga;
import com.ecosolicitud.shared.Rutas;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// Todo lo de terceros que la app muestra, con enlace a la obra exacta (no al
// perfil del autor), autor y licencia. Al sumar una foto o librería, va acá.
@Controller
class CreditosController {

    private static final String COMMONS = "https://commons.wikimedia.org/wiki/File:";
    private static final String CC0 = "https://creativecommons.org/publicdomain/zero/1.0/";
    private static final String CC_BY_2 = "https://creativecommons.org/licenses/by/2.0/";
    private static final String CC_BY_4 = "https://creativecommons.org/licenses/by/4.0/";
    private static final String CC_BY_SA_4 = "https://creativecommons.org/licenses/by-sa/4.0/";

    private static final List<Credito> FOTOS = List.of(
            new Credito("View of Rivera from Cerro Marconi", "Mx. Granger",
                    COMMONS + "View_of_Rivera_from_Cerro_Marconi.jpg", "CC0", CC0, "creditos.uso.portada"),
            new Credito("Border between Rivera and Santana do Livramento", "Mx. Granger",
                    COMMONS + "Border_between_Rivera_and_Santana_do_Livramento.jpg", "CC0", CC0,
                    "creditos.uso.error"),
            new Credito("Sorted waste containers close-up", "E961",
                    COMMONS + "Sorted_waste_containers_close-up.jpg", "CC BY 4.0", CC_BY_4,
                    "creditos.uso.guia.portada"),
            new Credito("Glass bottles next to glass recycling containers", "Donald Trung Quoc Don",
                    COMMONS + "Glass_bottles_next_to_glass_recycling_containers,_Hillegersberg,_Rotterdam_(2021)_02.jpg",
                    "CC BY-SA 4.0", CC_BY_SA_4, "creditos.uso.guia"),
            new Credito("Box baler at Walmart", "Walmart",
                    COMMONS + "Box_baler_at_Walmart.jpg", "CC BY 2.0", CC_BY_2, "creditos.uso.guia"),
            new Credito("Electronic junk separation in view of recycling", "Syced",
                    COMMONS + "Electronic_junk_separation_in_view_of_recycling.jpg", "CC0", CC0,
                    "creditos.uso.guia"),
            new Credito("Bales of PET bottles closeup", "Grendelkhan",
                    COMMONS + "Bales_of_PET_bottles_closeup.jpg", "CC BY-SA 4.0", CC_BY_SA_4,
                    "creditos.uso.guia.portada"),
            new Credito("Crushed cans", "Ruth Hartnup",
                    COMMONS + "Crushed_cans_(9691248780).jpg", "CC BY 2.0", CC_BY_2, "creditos.uso.guia"));

    private static final List<Credito> RECURSOS = List.of(
            new Credito("OpenStreetMap", "OpenStreetMap contributors",
                    "https://www.openstreetmap.org/copyright", "ODbL",
                    "https://opendatacommons.org/licenses/odbl/", "creditos.uso.mapa"),
            new Credito("Leaflet 1.9.4", "Volodymyr Agafonkin",
                    "https://leafletjs.com", "BSD-2-Clause",
                    "https://github.com/Leaflet/Leaflet/blob/main/LICENSE", "creditos.uso.mapa"),
            new Credito("htmx 2.0.4", "Big Sky Software",
                    "https://htmx.org", "0BSD",
                    "https://github.com/bigskysoftware/htmx/blob/master/LICENSE", "creditos.uso.htmx"),
            new Credito("Material Icons", "Google",
                    "https://fonts.google.com/icons", "Apache 2.0",
                    "https://www.apache.org/licenses/LICENSE-2.0", "creditos.uso.icono"));

    @GetMapping(Rutas.CREDITOS)
    String creditos(Model modelo) {
        modelo.addAttribute("fotos", FOTOS);
        modelo.addAttribute("recursos", RECURSOS);
        modelo.addAttribute("migaFinal", Miga.actualClave("creditos.titulo"));
        return "creditos";
    }

    record Credito(String obra, String autor, String url, String licencia, String licenciaUrl,
            String uso) {
    }
}
