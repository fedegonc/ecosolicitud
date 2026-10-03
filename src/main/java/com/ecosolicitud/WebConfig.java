package com.ecosolicitud;

import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.shared.ActorSesion;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfig implements WebMvcConfigurer {

    private final ActorSesion actor;

    WebConfig(ActorSesion actor) {
        this.actor = actor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RolSeccionInterceptor(actor))
                .addPathPatterns(Seccion.rutas());
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController(Rutas.RAIZ)
                .setViewName("redirect:" + Rutas.MIS_SOLICITUDES);
    }
}
