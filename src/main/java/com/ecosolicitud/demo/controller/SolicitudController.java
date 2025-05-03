package com.ecosolicitud.demo.controller;

import com.ecosolicitud.demo.model.Solicitud;
import com.ecosolicitud.demo.service.SolicitudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    @Autowired
    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping
    public String listarSolicitudes(Model model) {
        List<Solicitud> solicitudes = solicitudService.findAll();
        model.addAttribute("solicitudes", solicitudes);
        return "solicitudes/lista";
    }
}
