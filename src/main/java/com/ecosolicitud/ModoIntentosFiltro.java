package com.ecosolicitud;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.ecosolicitud.shared.Rutas;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

// Limita antes de BCrypt, también al cambiar de sesión. No confía en un
// X-Forwarded-For aportado por el cliente. El mapa tiene tamaño acotado.
final class ModoIntentosFiltro extends OncePerRequestFilter {

    private static final int DIRECCIONES = 1024;
    private static final Duration VENTANA = Duration.ofMinutes(1);

    private record Intento(Instant vence, int cantidad) {
    }

    private final Map<String, Intento> intentos = new HashMap<>();
    private final Clock reloj;
    private final int maximo;

    ModoIntentosFiltro(Clock reloj, int maximo) {
        this.reloj = reloj;
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
        if (!permitir(request.getRemoteAddr())) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Demasiados intentos. Espera un minuto e intenta de nuevo.");
            return;
        }
        chain.doFilter(request, response);
    }

    private synchronized boolean permitir(String direccion) {
        var ahora = reloj.instant();
        intentos.entrySet().removeIf(e -> !ahora.isBefore(e.getValue().vence()));
        var intento = intentos.get(direccion);
        if (intento == null) {
            if (intentos.size() >= DIRECCIONES) {
                return false;
            }
            intentos.put(direccion, new Intento(ahora.plus(VENTANA), 1));
            return true;
        }
        if (intento.cantidad() >= maximo) {
            return false;
        }
        intentos.put(direccion, new Intento(intento.vence(), intento.cantidad() + 1));
        return true;
    }
}
