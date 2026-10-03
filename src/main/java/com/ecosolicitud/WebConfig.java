package com.ecosolicitud;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfig implements WebMvcConfigurer {

    private final RolSesion rolSesion;

    WebConfig(RolSesion rolSesion) {
        this.rolSesion = rolSesion;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RolSeccionInterceptor(rolSesion))
                .addPathPatterns(Seccion.rutas());
    }
}
