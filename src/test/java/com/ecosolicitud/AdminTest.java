package com.ecosolicitud;

import java.util.regex.Pattern;

import com.ecosolicitud.cuenta.Cuentas;
import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Rutas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTest {

    private static final String PANEL_MODO = "/modo/dev";
    private static final String CLAVE = "clave-solo-de-pruebas";
    private static final String CENTRO = "acopio-verde";
    private static final String CENTRO_NOMBRE = "Microcentro";
    private static final String CUENTA_CENTRO = "renacer-norte";
    private static final String CLAVE_CENTRO = "centro-demo";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OrganizacionService organizaciones;

    @Autowired
    private Cuentas cuentas;

    @Test
    void sinPermisoNoSeEntraNiSeToca() throws Exception {
        mvc.perform(get(Rutas.ADMIN)).andExpect(status().isForbidden());
        var acceso = acceso();
        mvc.perform(post(Rutas.expandir(Rutas.ADMIN_CENTRO, CENTRO))
                        .session(acceso.sesion()).param("_csrf", acceso.csrf()))
                .andExpect(status().isForbidden());
        assertThat(organizaciones.buscar(CENTRO).orElseThrow().habilitada()).isTrue();
    }

    @Test
    void cuentaSinRolAdminRecibe403() throws Exception {
        var acceso = acceso();
        var resultado = mvc.perform(post("/cuenta/entrar").session(acceso.sesion())
                        .param("_csrf", acceso.csrf())
                        .param("username", CUENTA_CENTRO).param("password", CLAVE_CENTRO))
                .andExpect(status().is3xxRedirection()).andReturn();
        var sesion = (MockHttpSession) resultado.getRequest().getSession();
        mvc.perform(get(Rutas.ADMIN).session(sesion)).andExpect(status().isForbidden());
    }

    @Test
    void elDestinoSaleDelRolNoDelFormulario() throws Exception {
        var acceso = acceso();
        var resultado = mvc.perform(post(Rutas.CUENTA_ENTRAR).session(acceso.sesion())
                        .param("_csrf", acceso.csrf())
                        .param("username", "admin-test").param("password", CLAVE))
                .andExpect(redirectedUrl(Rutas.ADMIN)).andReturn();
        var sesion = (MockHttpSession) resultado.getRequest().getSession();
        mvc.perform(get(Rutas.ADMIN).session(sesion)).andExpect(status().isOk());
    }

    @Test
    void adminVeTotalesCentrosYCuentas() throws Exception {
        var admin = admin();
        mvc.perform(get(Rutas.ADMIN).session(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Administración")))
                .andExpect(content().string(containsString(CENTRO_NOMBRE)))
                .andExpect(content().string(containsString(CUENTA_CENTRO)));
    }

    @Test
    void elBotonCambiaElEstadoYElCentroDejaDeVerse() throws Exception {
        var admin = admin();
        String csrf = csrf(mvc.perform(get(Rutas.ADMIN).session(admin)).andReturn());
        try {
            mvc.perform(post(Rutas.expandir(Rutas.ADMIN_CENTRO, CENTRO)).session(admin)
                            .param("_csrf", csrf))
                    .andExpect(redirectedUrl(Rutas.ADMIN));
            assertThat(organizaciones.buscar(CENTRO).orElseThrow().habilitada()).isFalse();
            // ni el mapa de acopios ni el formulario de solicitud lo listan
            assertThat(organizaciones.enCiudad(Ciudad.RIVERA))
                    .noneMatch(o -> o.id().equals(CENTRO));
            mvc.perform(get(Rutas.ACOPIOS))
                    .andExpect(content().string(not(containsString(CENTRO_NOMBRE))));
        } finally {
            if (organizaciones.buscar(CENTRO).map(o -> !o.habilitada()).orElse(false)) {
                organizaciones.cambiarHabilitada(CENTRO);
            }
        }
        mvc.perform(get(Rutas.ACOPIOS))
                .andExpect(content().string(containsString(CENTRO_NOMBRE)));
    }

    @Test
    void cuentaDeBajaNoPuedeEntrar() throws Exception {
        var admin = admin();
        String csrf = csrf(mvc.perform(get(Rutas.ADMIN).session(admin)).andReturn());
        var cuenta = cuentas.usuarios().stream()
                .filter(u -> u.nombre().equals(CUENTA_CENTRO)).findFirst().orElseThrow();
        try {
            mvc.perform(post(Rutas.expandir(Rutas.ADMIN_USUARIO, cuenta.id())).session(admin)
                            .param("_csrf", csrf))
                    .andExpect(redirectedUrl(Rutas.ADMIN));
            var acceso = acceso();
            mvc.perform(post("/cuenta/entrar").session(acceso.sesion())
                            .param("_csrf", acceso.csrf())
                            .param("username", CUENTA_CENTRO).param("password", CLAVE_CENTRO))
                    .andExpect(redirectedUrl("/cuenta/entrar?error"));
        } finally {
            if (cuentas.usuarios().stream().filter(u -> u.id().equals(cuenta.id()))
                    .findFirst().map(u -> !u.activo()).orElse(false)) {
                cuentas.cambiarActivo(cuenta.id());
            }
        }
    }

    private Acceso acceso() throws Exception {
        var pagina = mvc.perform(get(PANEL_MODO)).andExpect(status().isOk()).andReturn();
        return new Acceso((MockHttpSession) pagina.getRequest().getSession(), csrf(pagina));
    }

    private MockHttpSession admin() throws Exception {
        var acceso = acceso();
        var resultado = mvc.perform(post(PANEL_MODO).session(acceso.sesion())
                        .param("_csrf", acceso.csrf())
                        .param("username", "admin-test").param("password", CLAVE))
                .andExpect(redirectedUrl(Rutas.ADMIN)).andReturn();
        return (MockHttpSession) resultado.getRequest().getSession();
    }

    private static String csrf(MvcResult pagina) throws Exception {
        var matcher = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"")
                .matcher(pagina.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private record Acceso(MockHttpSession sesion, String csrf) {
    }
}
