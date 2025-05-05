package com.ecosolicitud.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Bean;

@Configuration
public class AppConfig {
    // Bean passwordEncoder eliminado para resolver conflicto con SecurityConfig
}
