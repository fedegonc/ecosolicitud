package com.ecosolicitud;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// /design dibuja la portada esqueleto por ciudad; el contenido vive en DesignDatos.
@Controller
class DesignController {

    @GetMapping("/design")
    String design(@RequestParam(defaultValue = "rivera") String ciudad, Model model) {
        model.addAttribute("pagina", DesignDatos.para(ciudad));
        return "design";
    }
}
