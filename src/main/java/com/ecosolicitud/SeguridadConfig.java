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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Clock;
import java.util.function.Function;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.LocaleResolver;

import com.ecosolicitud.shared.Rutas;
import com.ecosolicitud.shared.Volver;

@Configuration
@EnableWebSecurity
class SeguridadConfig {

    @Bean
    @org.springframework.core.annotation.Order(1)
    SecurityFilterChain seguridadCuentas(HttpSecurity http,
            AuthenticationSuccessHandler ingreso, MessageSource mensajes, LocaleResolver idiomas,
            @Value("${ecosolicitud.cuenta.intentos:20}") int intentosCuenta)
            throws Exception {
        return http.securityMatcher("/cuenta/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(Rutas.CUENTA_REGISTRAR, Rutas.CUENTA_ENTRAR,
                                Rutas.CUENTA_CONDICIONES, Rutas.CUENTA_RECUPERAR,
                                Rutas.CUENTA_RECUPERAR_VERIFICAR).permitAll()
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
                        intentosCuenta, avisoIntentos(mensajes, idiomas)),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, MessageSource mensajes, LocaleResolver idiomas,
            @Value("${ecosolicitud.demo.habilitada:true}") boolean demoHabilitada,
            @Value("${ecosolicitud.modo.intentos:5}") int intentos) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(Rutas.DEMO_REINICIAR).denyAll()
                        .requestMatchers(Rutas.NUEVA + "/**", Rutas.NUEVA,
                                Rutas.MIS_SOLICITUDES + "/**", Rutas.MIS_SOLICITUDES,
                                Rutas.AVISOS + "/**", Rutas.AVISOS,
                                Rutas.OPINION + "/**", Rutas.OPINION, "/metricas").authenticated()
                        .requestMatchers("/demo/**", "/org/**").access((usuario, contexto) ->
                                new AuthorizationDecision(demoHabilitada && usuario.get().getAuthorities()
                                        .stream().anyMatch(a -> a.getAuthority().equals("ROLE_DEMO")
                                                || a.getAuthority().equals("ROLE_ORGANIZACION"))))
                        .anyRequest().permitAll())
                .exceptionHandling(errores -> errores
                        .accessDeniedHandler(new FormularioVencido())
                        // los privados del ciudadano redirigen a entrar; el resto queda 403
                        .defaultAuthenticationEntryPointFor(aIngresar(),
                                new OrRequestMatcher(
                                        new AntPathRequestMatcher(Rutas.NUEVA + "/**"),
                                        new AntPathRequestMatcher(Rutas.NUEVA),
                                        new AntPathRequestMatcher(Rutas.MIS_SOLICITUDES + "/**"),
                                        new AntPathRequestMatcher(Rutas.MIS_SOLICITUDES),
                                        new AntPathRequestMatcher(Rutas.AVISOS + "/**"),
                                        new AntPathRequestMatcher(Rutas.AVISOS),
                                        new AntPathRequestMatcher(Rutas.OPINION + "/**"),
                                        new AntPathRequestMatcher(Rutas.OPINION)))
                        // el resto, 403. Como default y no con authenticationEntryPoint(),
                        // que reemplaza los defaults y dejaba también a los privados en 403
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.FORBIDDEN),
                                AnyRequestMatcher.INSTANCE))
                .formLogin(login -> login.loginPage(Rutas.MODO_DEV)
                        .loginProcessingUrl(Rutas.MODO_DEV)
                        .defaultSuccessUrl(Rutas.RAIZ, true)
                        .failureUrl(Rutas.MODO_DEV + "?error"))
                .logout(salir -> salir.logoutUrl(Rutas.MODO_SALIR)
                        .logoutSuccessUrl(Rutas.RAIZ)
                        .invalidateHttpSession(true).deleteCookies("JSESSIONID"))
                .requestCache(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(new ModoIntentosFiltro(Clock.systemUTC(), intentos,
                        avisoIntentos(mensajes, idiomas)),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // a Ingresar, recordando la página pedida para volver después de entrar.
    // Un fragmento htmx (sesión vencida o servidor reiniciado con la página
    // abierta) no se redirige con 302: htmx lo seguiría y pegaría Ingresar
    // dentro del fragmento o vaciaría #flujo. Con HX-Redirect navega la página.
    private static AuthenticationEntryPoint aIngresar() {
        var ingresar = new LoginUrlAuthenticationEntryPoint(Rutas.CUENTA_ENTRAR);
        return (request, response, error) -> {
            // el contador de avisos se pide solo al volver a la pestaña: es
            // cortesía, no acción del usuario; un 403 silencioso y la próxima
            // navegación real es la que pide ingreso
            if (request.getRequestURI().startsWith(Rutas.AVISOS_CONTADOR)) {
                response.sendError(HttpStatus.FORBIDDEN.value());
                return;
            }
            if (Volver.esFragmento(request)) {
                Volver.guardarPagina(request);
                response.setHeader("HX-Redirect", request.getContextPath() + Rutas.CUENTA_ENTRAR);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }
            Volver.guardar(request);
            ingresar.commence(request, response, error);
        };
    }

    private static Function<HttpServletRequest, String> avisoIntentos(MessageSource mensajes,
            LocaleResolver idiomas) {
        return r -> mensajes.getMessage("modo.intentos", null, idiomas.resolveLocale(r));
    }

    @Bean
    PasswordEncoder claves() {
        return new BCryptPasswordEncoder();
    }

}
