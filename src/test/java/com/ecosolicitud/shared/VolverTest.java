package com.ecosolicitud.shared;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class VolverTest {

    @Test
    void soloRutasInternasDeNavegacion() {
        assertThat(Volver.interna("/nueva?ciudad=RIVERA")).isTrue();
        assertThat(Volver.interna("//otra.web/x")).isFalse();
        assertThat(Volver.interna("https://otra.web")).isFalse();
        assertThat(Volver.interna("/\\otra.web")).isFalse();
        assertThat(Volver.interna(Rutas.AVISOS_CONTADOR)).isFalse();
    }

    @Test
    void seGuardaUnaVezYNoGuardaFragmentosHtmxNiPost() {
        var get = new MockHttpServletRequest("GET", "/mis-solicitudes");
        Volver.guardar(get);
        assertThat(Volver.tomar(get)).contains("/mis-solicitudes");
        assertThat(Volver.tomar(get)).isEmpty();

        var fragmento = new MockHttpServletRequest("GET", "/avisos");
        fragmento.addHeader("HX-Request", "true");
        Volver.guardar(fragmento);
        assertThat(Volver.tomar(fragmento)).isEmpty();

        var boost = new MockHttpServletRequest("GET", "/avisos");
        boost.addHeader("HX-Request", "true");
        boost.addHeader("HX-Boosted", "true");
        Volver.guardar(boost);
        assertThat(Volver.tomar(boost)).contains("/avisos");

        var post = new MockHttpServletRequest("POST", "/nueva");
        Volver.guardar(post);
        assertThat(Volver.tomar(post)).isEmpty();
    }
}
