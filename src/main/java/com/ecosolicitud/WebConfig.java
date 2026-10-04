package com.ecosolicitud;

import com.ecosolicitud.shared.ActorSesion;

import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

@Configuration
class WebConfig implements WebMvcConfigurer {

    private final ActorSesion actor;

    WebConfig(ActorSesion actor) {
        this.actor = actor;
    }

    @Bean
    LocaleResolver localeResolver() {
        var resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.forLanguageTag("es"));
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RolSeccionInterceptor(actor))
                .addPathPatterns(Seccion.rutas());
        var lang = new LocaleChangeInterceptor();
        lang.setParamName("lang");
        registry.addInterceptor(lang);
    }

}
