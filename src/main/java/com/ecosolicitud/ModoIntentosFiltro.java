package com.ecosolicitud;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.ecosolicitud.shared.Rutas;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

// Limita antes de BCrypt, también al cambiar de sesión. No confía en un
// X-Forwarded-For aportado por el cliente. El mapa tiene tamaño acotado.
// Cuenta solo los ingresos fallidos (redirect a ?error): muchas personas
// detrás de la misma wifi comparten dirección y no deben bloquearse entre
// sí por entrar bien.
final class ModoIntentosFiltro extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ModoIntentosFiltro.class);
    private static final int DIRECCIONES = 1024;
    private static final Duration VENTANA = Duration.ofMinutes(1);

    private record Intento(Instant vence, int cantidad) {
    }

    private final Map<String, Intento> intentos = new HashMap<>();
    private final Clock reloj;
    private final int maximo;
    // el texto sale de messages en el idioma de la sesión, no de este archivo
    private final Function<HttpServletRequest, String> aviso;

    ModoIntentosFiltro(Clock reloj, int maximo, Function<HttpServletRequest, String> aviso) {
        this.reloj = reloj;
        this.aviso = aviso;
        if (maximo < 1) {
            throw new IllegalArgumentException("El limite de intentos debe ser positivo");
        }
        this.maximo = maximo;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var ruta = request.getRequestURI().substring(request.getContextPath().length());
        return !"POST".equals(request.getMethod())
                || !(Rutas.MODO_DEV.equals(ruta) || Rutas.CUENTA_ENTRAR.equals(ruta));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var direccion = request.getRemoteAddr();
        if (!permitir(direccion)) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(aviso.apply(request));
            return;
        }
        chain.doFilter(request, response);
        var destino = response.getHeader("Location");
        if (destino != null && destino.contains("?error")) {
            registrarFallo(direccion);
        }
    }

    private synchronized boolean permitir(String direccion) {
        var ahora = reloj.instant();
        intentos.entrySet().removeIf(e -> !ahora.isBefore(e.getValue().vence()));
        var intento = intentos.get(direccion);
        if (intento == null) {
            // mapa lleno: no se puede llevar la cuenta de una dirección nueva
            return intentos.size() < DIRECCIONES;
        }
        return intento.cantidad() < maximo;
    }

    private synchronized void registrarFallo(String direccion) {
        var intento = intentos.get(direccion);
        if (intento == null) {
            if (intentos.size() < DIRECCIONES) {
                intentos.put(direccion, new Intento(reloj.instant().plus(VENTANA), 1));
            }
            return;
        }
        intentos.put(direccion, new Intento(intento.vence(), intento.cantidad() + 1));
        // una sola línea por dirección y ventana, sin la IP: queda la evidencia
        // de un ataque sin registrar datos de nadie
        if (intento.cantidad() + 1 == maximo) {
            LOG.warn("ingreso bloqueado 1 min: {} fallos seguidos desde una misma dirección", maximo);
        }
    }
}
