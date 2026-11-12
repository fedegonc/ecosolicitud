package com.ecosolicitud;

import com.ecosolicitud.cuenta.Cuentas;
import com.ecosolicitud.cuenta.Usuario;
import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Rutas;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

// Panel de administración: totales y los dos interruptores (centro y
// cuenta). Solo ROLE_ADMIN llega hasta acá (SeguridadConfig); las acciones
// alternan el estado y vuelven a la misma página.
@Controller
class AdminController {

    private final OrganizacionService organizaciones;
    private final Cuentas cuentas;

    AdminController(OrganizacionService organizaciones, Cuentas cuentas) {
        this.organizaciones = organizaciones;
        this.cuentas = cuentas;
    }

    @GetMapping(Rutas.ADMIN)
    String panel(Model modelo) {
        var centros = organizaciones.todas();
        var usuarios = cuentas.usuarios();
        modelo.addAttribute("centros", centros);
        modelo.addAttribute("usuarios", usuarios);
        modelo.addAttribute("habilitados",
                centros.stream().filter(OrganizacionInfo::habilitada).count());
        modelo.addAttribute("activos",
                usuarios.stream().filter(Usuario::activo).count());
        return "secciones/admin";
    }

    @PostMapping(Rutas.ADMIN_CENTRO)
    String centro(@PathVariable String id) {
        organizaciones.cambiarHabilitada(id);
        return "redirect:" + Rutas.ADMIN;
    }

    @PostMapping(Rutas.ADMIN_USUARIO)
    String usuario(@PathVariable String id) {
        cuentas.cambiarActivo(id);
        return "redirect:" + Rutas.ADMIN;
    }
}
