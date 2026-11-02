package com.ecosolicitud;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import com.ecosolicitud.shared.Rutas;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Reemplaza al access log de Tomcat en prod con solo lo importante:
// - INFO: una línea por pedido en /estadisticas, /mis-solicitudes y /avisos
//   (sin /avisos/contador, que se pide solo al volver a la pestaña);
// - WARN: cualquier pedido lento (>= 1 s) o con error 5xx, en cualquier ruta.
// Método, ruta, estado y milisegundos. Sin query, IP, User-Agent ni usuario;
// los números de la ruta salen como {id}. Se apaga con
// logging.level.ecosolicitud.tiempos=OFF.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
final class TiemposFiltro extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger("ecosolicitud.tiempos");
    private static final List<String> MEDIDAS =
            List.of(Rutas.ESTADISTICAS, Rutas.MIS_SOLICITUDES, Rutas.AVISOS);
    private static final long LENTO_MS = 1000;
    private static final Pattern NUMERO = Pattern.compile("/\\d+(?=/|$)");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LOG.isWarnEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        long inicio = System.nanoTime();
        boolean excepcion = true;
        try {
            chain.doFilter(request, response);
            excepcion = false;
        } finally {
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            var ruta = ruta(request);
            // una excepción sin manejar todavía no puso el 500 en la respuesta
            int estado = excepcion ? 500 : response.getStatus();
            if (ms >= LENTO_MS || estado >= 500) {
                LOG.warn("{} {} {} {}ms", request.getMethod(), anonima(ruta), estado, ms);
            } else if (medida(ruta)) {
                LOG.info("{} {} {} {}ms", request.getMethod(), anonima(ruta), estado, ms);
            }
        }
    }

    static boolean medida(String ruta) {
        return !ruta.startsWith(Rutas.AVISOS_CONTADOR)
                && MEDIDAS.stream().anyMatch(m -> ruta.equals(m) || ruta.startsWith(m + "/"));
    }

    static String anonima(String ruta) {
        return NUMERO.matcher(ruta).replaceAll("/{id}");
    }

    private static String ruta(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
