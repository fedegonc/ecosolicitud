package com.ecosolicitud;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Clock;

import com.ecosolicitud.shared.Rutas;

@Configuration
@EnableWebSecurity
class SeguridadConfig {

    @Bean
    @org.springframework.core.annotation.Order(1)
    SecurityFilterChain seguridadCuentas(HttpSecurity http,
            AuthenticationSuccessHandler ingreso,
            @Value("${ecosolicitud.cuenta.intentos:5}") int intentosCuenta)
            throws Exception {
        return http.securityMatcher("/cuenta/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(Rutas.CUENTA_REGISTRAR, Rutas.CUENTA_ENTRAR,
                                Rutas.CUENTA_CONDICIONES).permitAll()
                        .anyRequest().authenticated())
                .formLogin(login -> login.loginPage(Rutas.CUENTA_ENTRAR)
                        .loginProcessingUrl(Rutas.CUENTA_ENTRAR)
                        .successHandler(ingreso)
                        .failureUrl(Rutas.CUENTA_ENTRAR + "?error"))
                .logout(salir -> salir.logoutUrl(Rutas.CUENTA_SALIR)
                        .logoutSuccessUrl(Rutas.RAIZ)
                        .invalidateHttpSession(true).deleteCookies("JSESSIONID"))
                .requestCache(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(new ModoIntentosFiltro(Clock.systemUTC(),
                        intentosCuenta),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http,
            @Value("${ecosolicitud.demo.habilitada:true}") boolean demoHabilitada,
            @Value("${ecosolicitud.modo.intentos:5}") int intentos) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(Rutas.DEMO_REINICIAR).denyAll()
                        .requestMatchers(Rutas.NUEVA + "/**", Rutas.NUEVA,
                                Rutas.MIS_SOLICITUDES + "/**", Rutas.MIS_SOLICITUDES,
                                Rutas.AVISOS + "/**", Rutas.AVISOS,
                                Rutas.OPINION + "/**", Rutas.OPINION).authenticated()
                        .requestMatchers("/demo/**", "/org/**").access((usuario, contexto) ->
                                new AuthorizationDecision(demoHabilitada && usuario.get().getAuthorities()
                                        .stream().anyMatch(a -> a.getAuthority().equals("ROLE_DEMO")
                                                || a.getAuthority().equals("ROLE_ORGANIZACION"))))
                        .anyRequest().permitAll())
                .exceptionHandling(errores -> errores
                        // los privados del ciudadano redirigen a entrar; el resto queda 403
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint(Rutas.CUENTA_ENTRAR),
                                new OrRequestMatcher(
                                        new AntPathRequestMatcher(Rutas.NUEVA + "/**"),
                                        new AntPathRequestMatcher(Rutas.NUEVA),
                                        new AntPathRequestMatcher(Rutas.MIS_SOLICITUDES + "/**"),
                                        new AntPathRequestMatcher(Rutas.MIS_SOLICITUDES),
                                        new AntPathRequestMatcher(Rutas.AVISOS + "/**"),
                                        new AntPathRequestMatcher(Rutas.AVISOS),
                                        new AntPathRequestMatcher(Rutas.OPINION + "/**"),
                                        new AntPathRequestMatcher(Rutas.OPINION)))
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.FORBIDDEN)))
                .formLogin(login -> login.loginPage(Rutas.MODO_DEV)
                        .loginProcessingUrl(Rutas.MODO_DEV)
                        .defaultSuccessUrl(Rutas.RAIZ, true)
                        .failureUrl(Rutas.MODO_DEV + "?error"))
                .logout(salir -> salir.logoutUrl(Rutas.MODO_SALIR)
                        .logoutSuccessUrl(Rutas.RAIZ)
                        .invalidateHttpSession(true).deleteCookies("JSESSIONID"))
                .requestCache(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(new ModoIntentosFiltro(Clock.systemUTC(), intentos),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder claves() {
        return new BCryptPasswordEncoder();
    }

}
