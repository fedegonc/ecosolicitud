package com.ecosolicitud.demo.config;

import com.ecosolicitud.demo.roles.api.RolPermissionEvaluator;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;

@Configuration
public class ThymeleafConfig implements WebMvcConfigurer {
    
    @Bean
    public SpringTemplateEngine templateEngine(SpringResourceTemplateResolver templateResolver) {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        templateEngine.addDialect(new LayoutDialect());
        
        // Agregar el dialecto de Spring Security para usar sec:authorize en las plantillas
        templateEngine.addDialect(new org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect());
        
        return templateEngine;
    }
}
