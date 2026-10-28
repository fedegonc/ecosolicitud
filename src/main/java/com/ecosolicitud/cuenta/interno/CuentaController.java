package com.ecosolicitud.cuenta.interno;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ecosolicitud.cuenta.CuentaException;
import com.ecosolicitud.cuenta.Cuentas;
import com.ecosolicitud.cuenta.Pais;
import com.ecosolicitud.shared.Rutas;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class CuentaController {

    private final Cuentas cuentas;

    CuentaController(Cuentas cuentas) {
        this.cuentas = cuentas;
    }

    @GetMapping(Rutas.CUENTA_CONDICIONES)
    String condiciones() {
        return "secciones/cuenta/condiciones";
    }

    @GetMapping(Rutas.CUENTA_ENTRAR)
    String entrar() {
        return "secciones/cuenta/entrar";
    }

    @GetMapping(Rutas.CUENTA_REGISTRAR)
    String registrar(Model modelo) {
        modelo.addAttribute("errores", Map.of());
        return "secciones/cuenta/registrar";
    }

    @PostMapping(Rutas.CUENTA_REGISTRAR)
    String crear(@RequestParam String nombre, @RequestParam String pais,
            @RequestParam String telefono, @RequestParam String clave,
            @RequestParam(required = false) String condiciones, Model modelo) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (condiciones == null) {
            errores.put("condiciones", "Tenés que aceptar las condiciones de uso");
        }
        Pais p;
        try {
            p = Pais.valueOf(pais == null ? "" : pais);
        } catch (IllegalArgumentException e) {
            p = null;
            errores.put("pais", "Elegí Uruguay o Brasil");
        }
        if (p != null) {
            try {
                cuentas.validarDatos(nombre, telefono, p, clave);
            } catch (CuentaException e) {
                errores.put(e.campo(), e.getMessage());
            }
        }
        if (errores.isEmpty()) {
            cuentas.registrarCiudadano(nombre.strip(), telefono, p, clave);
            modelo.addAttribute("registrada", true);
        }
        modelo.addAttribute("errores", errores);
        return "secciones/cuenta/registrar";
    }
}
