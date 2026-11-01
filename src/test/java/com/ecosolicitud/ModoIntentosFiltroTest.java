package com.ecosolicitud;

import java.time.Clock;
import java.time.Instant;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModoIntentosFiltroTest {

    @Test
    void noSeEludeConForwardedForYSeLiberaAlMinuto() throws Exception {
        var reloj = mock(Clock.class);
        var inicio = Instant.parse("2026-10-27T12:00:00Z");
        when(reloj.instant()).thenReturn(inicio);
        var filtro = new ModoIntentosFiltro(reloj, 5, r -> "");
        for (int i = 0; i < 5; i++) {
            assertThat(enviar(filtro, "127.0.0.1", "cliente-" + i).getStatus()).isEqualTo(302);
        }
        var bloqueado = enviar(filtro, "127.0.0.1", "otra-direccion");
        assertThat(bloqueado.getStatus()).isEqualTo(429);
        assertThat(bloqueado.getHeader("Retry-After")).isEqualTo("60");
        assertThat(enviar(filtro, "127.0.0.2", "otro").getStatus()).isEqualTo(302);
        when(reloj.instant()).thenReturn(inicio.plusSeconds(60));
        assertThat(enviar(filtro, "127.0.0.1", "otro").getStatus()).isEqualTo(302);
    }

    @Test
    void laMemoriaEstaAcotadaSinExpulsarBloqueosActivos() throws Exception {
        var filtro = new ModoIntentosFiltro(Clock.systemUTC(), 5, r -> "");
        for (int i = 0; i < 1024; i++) {
            assertThat(enviar(filtro, "cliente-" + i, "").getStatus()).isEqualTo(302);
        }
        assertThat(enviar(filtro, "nuevo", "").getStatus()).isEqualTo(429);
    }

    @Test
    void noLimitaLaNavegacion() throws Exception {
        var filtro = new ModoIntentosFiltro(Clock.systemUTC(), 5, r -> "");
        var request = new MockHttpServletRequest("GET", "/modo/dev");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        filtro.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void losIngresosCorrectosNoConsumenElCupoDeLaDireccion() throws Exception {
        // 200 personas detrás de la misma wifi entrando bien
        var filtro = new ModoIntentosFiltro(Clock.systemUTC(), 5, r -> "");
        for (int i = 0; i < 200; i++) {
            assertThat(enviar(filtro, "10.0.0.1", "", false).getStatus()).isEqualTo(302);
        }
        for (int i = 0; i < 5; i++) {
            enviar(filtro, "10.0.0.1", "", true);
        }
        assertThat(enviar(filtro, "10.0.0.1", "", false).getStatus()).isEqualTo(429);
    }

    // por defecto, un ingreso fallido (redirect a ?error, como Spring Security)
    private MockHttpServletResponse enviar(ModoIntentosFiltro filtro, String direccion,
            String forwarded) throws Exception {
        return enviar(filtro, direccion, forwarded, true);
    }

    private MockHttpServletResponse enviar(ModoIntentosFiltro filtro, String direccion,
            String forwarded, boolean falla) throws Exception {
        var request = new MockHttpServletRequest("POST", "/modo/dev");
        request.setRemoteAddr(direccion);
        request.addHeader("X-Forwarded-For", forwarded);
        var response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res)
                .sendRedirect(falla ? "/modo/dev?error" : "/");
        filtro.doFilter(request, response, chain);
        return response;
    }
}
