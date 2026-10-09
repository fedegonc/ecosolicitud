package com.ecosolicitud.demo;

import com.ecosolicitud.comunidad.ComunidadService;
import com.ecosolicitud.guia.GuiaService;
import com.ecosolicitud.opinion.OpinionService;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.CatalogoMateriales;
import com.ecosolicitud.solicitud.AvisoService;
import com.ecosolicitud.solicitud.SolicitudService;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoService implements ApplicationRunner {

    private final OrganizacionService organizaciones;
    private final SolicitudService solicitudes;
    private final AvisoService avisos;
    private final GuiaService guia;
    private final ComunidadService comunidad;
    private final OpinionService opiniones;
    private final CatalogoMateriales catalogo;

    public DemoService(OrganizacionService organizaciones,
            SolicitudService solicitudes, AvisoService avisos, GuiaService guia,
            ComunidadService comunidad, OpinionService opiniones,
            CatalogoMateriales catalogo) {
        this.organizaciones = organizaciones;
        this.solicitudes = solicitudes;
        this.avisos = avisos;
        this.guia = guia;
        this.comunidad = comunidad;
        this.opiniones = opiniones;
        this.catalogo = catalogo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (organizaciones.vacia()) {
            reiniciar();
        }
    }

    @Transactional
    public void reiniciar() {
        organizaciones.reemplazarTodas(DemoDatos.organizaciones(catalogo));
        solicitudes.reemplazarTodas(DemoDatos.solicitudes(catalogo));
        avisos.reemplazarAvisos(DemoDatos.avisos(solicitudes.todas()));
        guia.reemplazarTodos(DemoGuia.guia(catalogo));
        comunidad.reemplazarTodas(DemoDatos.comunidad());
        opiniones.reemplazarTodas(DemoDatos.opiniones());
        comunidad.reemplazarRecicladores(DemoDatos.recicladores());
    }
}
