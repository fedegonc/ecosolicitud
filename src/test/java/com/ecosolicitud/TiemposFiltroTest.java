package com.ecosolicitud;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class TiemposFiltroTest {

    @Test
    void soloMideLasTresPaginasYSusSubrutas() {
        assertThat(TiemposFiltro.medida("/estadisticas")).isTrue();
        assertThat(TiemposFiltro.medida("/mis-solicitudes")).isTrue();
        assertThat(TiemposFiltro.medida("/mis-solicitudes/informe")).isTrue();
        assertThat(TiemposFiltro.medida("/avisos/contador")).isFalse();
        assertThat(TiemposFiltro.medida("/avisos")).isTrue();
        assertThat(TiemposFiltro.medida("/")).isFalse();
        assertThat(TiemposFiltro.medida("/nueva")).isFalse();
        assertThat(TiemposFiltro.medida("/css/app.css")).isFalse();
        assertThat(TiemposFiltro.medida("/avisos-falsos")).isFalse();
    }

    @Test
    void lasRutasNoLlevanIdentificadores() {
        assertThat(TiemposFiltro.anonima("/mis-solicitudes/1234")).isEqualTo("/mis-solicitudes/{id}");
        assertThat(TiemposFiltro.anonima("/mis-solicitudes/12/cancelar"))
                .isEqualTo("/mis-solicitudes/{id}/cancelar");
        assertThat(TiemposFiltro.anonima("/estadisticas")).isEqualTo("/estadisticas");
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void registraLasMedidasYCualquierErrorPeroNoElRestoNiLaQuery(CapturedOutput salida)
            throws Exception {
        var filtro = new TiemposFiltro();
        var medida = new MockHttpServletRequest("GET", "/mis-solicitudes/77");
        medida.setQueryString("telefono=099123456");
        filtro.doFilter(medida, new MockHttpServletResponse(), mock(FilterChain.class));
        filtro.doFilter(new MockHttpServletRequest("GET", "/nueva"), new MockHttpServletResponse(),
                mock(FilterChain.class));

        var roto = mock(FilterChain.class);
        doThrow(new IllegalStateException("x")).when(roto).doFilter(any(), any());
        assertThatThrownBy(() -> filtro.doFilter(new MockHttpServletRequest("POST", "/nueva"),
                new MockHttpServletResponse(), roto)).isInstanceOf(IllegalStateException.class);

        assertThat(salida.getOut()).contains("GET /mis-solicitudes/{id} 200")
                .contains("POST /nueva 500")
                .doesNotContain("GET /nueva ")
                .doesNotContain("099123456").doesNotContain("/77");
    }
}
