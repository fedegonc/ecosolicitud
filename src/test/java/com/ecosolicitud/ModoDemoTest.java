package com.ecosolicitud;

import java.util.regex.Pattern;

import com.ecosolicitud.shared.ActorSesion;
import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.shared.Rutas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ModoDemoTest {

    private static final String PANEL = "/modo/dev";
    private static final String SALIR = "/modo/dev/salir";
    private static final String CLAVE = "clave-solo-de-pruebas";

    @Autowired
    private MockMvc mvc;

    @DynamicPropertySource
    static void credenciales(DynamicPropertyRegistry registry) {
        var hash = new BCryptPasswordEncoder().encode(CLAVE);
        registry.add("ecosolicitud.modo.usuario", () -> "admin-test");
        registry.add("ecosolicitud.modo.clave-hash", () -> hash);
        registry.add("ecosolicitud.modo.intentos", () -> 5);
    }

    @Test
    void visitanteNoVeControlesNiEnlacesDemo() throws Exception {
        mvc.perform(get(Rutas.RAIZ))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("class=\"rol-switch\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("overleaf.com"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("docs.google.com/presentation"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(Rutas.CREDITOS)));
    }

    @Test
    void visitanteNoPuedeCambiarRolNiCentroAunqueTengaCsrf() throws Exception {
        var acceso = acceso("visitante");
        for (var ruta : new String[]{Rutas.DEMO_ROL, Rutas.DEMO_ORGANIZACION}) {
            mvc.perform(post(ruta).session(acceso.sesion()).param("_csrf", acceso.csrf())
                            .param("rol", "ORGANIZACION").param("id", "frontera-limpia"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void claveIncorrectaNoActivaDemoYElErrorEsGenerico() throws Exception {
        var acceso = acceso("incorrecta");
        mvc.perform(ingreso(acceso, "admin-test", "incorrecta"))
                .andExpect(redirectedUrl(PANEL + "?error"));
        mvc.perform(ingreso(acceso, "otro", CLAVE))
                .andExpect(redirectedUrl(PANEL + "?error"));
        mvc.perform(get(Rutas.RAIZ).session(acceso.sesion()))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("class=\"rol-switch\""))));
    }

    @Test
    void ingresoRotaIdDeSesionYNoAfectaOtroVisitante() throws Exception {
        var acceso = acceso("aislamiento");
        String anterior = acceso.sesion().getId();
        var admin = autenticar(acceso);
        assertThat(admin.getId()).isNotEqualTo(anterior);
        mvc.perform(get(PANEL).session(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(SALIR)));
        mvc.perform(get(Rutas.RAIZ).session(admin))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"rol-switch\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("overleaf.com")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(Rutas.DEMO_REINICIAR))));
        mvc.perform(get(Rutas.RAIZ))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("class=\"rol-switch\""))));
    }

    @Test
    void adminCambiaRolYCentroPeroSalirEliminaLaIdentidadSimulada() throws Exception {
        var admin = autenticar(acceso("salida"));
        String csrf = csrf(mvc.perform(get(PANEL).session(admin)).andReturn());
        mvc.perform(post(Rutas.DEMO_ROL).session(admin).param("_csrf", csrf)
                        .param("rol", "ORGANIZACION"))
                .andExpect(redirectedUrl(Rutas.ORG_SOLICITUDES));
        mvc.perform(post(Rutas.DEMO_ORGANIZACION).session(admin).param("_csrf", csrf)
                        .param("id", "frontera-limpia"))
                .andExpect(status().is3xxRedirection());
        var actor = (ActorSesion) admin.getAttribute("scopedTarget.actorSesion");
        assertThat(actor.actual().organizacionId()).isEqualTo("frontera-limpia");
        mvc.perform(post(SALIR).session(admin).param("_csrf", csrf))
                .andExpect(redirectedUrl(Rutas.RAIZ));
        assertThat(admin.isInvalid()).isTrue();
        var nueva = mvc.perform(get(Rutas.RAIZ)).andReturn();
        var visitante = (ActorSesion) nueva.getRequest().getSession()
                .getAttribute("scopedTarget.actorSesion");
        assertThat(visitante.get()).isEqualTo(Rol.CIUDADANO);
        assertThat(visitante.actual().organizacionId()).isNull();
        var acceso = acceso("despues-de-salir");
        mvc.perform(post(Rutas.DEMO_ROL).session(acceso.sesion()).param("_csrf", acceso.csrf())
                        .param("rol", "ORGANIZACION"))
                .andExpect(status().isForbidden());
    }

    @Test
    void csrfSeExigeEnIngresoCambioDeRolYSalida() throws Exception {
        mvc.perform(post(PANEL).param("username", "admin-test").param("password", CLAVE))
                .andExpect(status().isForbidden());
        var admin = autenticar(acceso("csrf"));
        mvc.perform(post(Rutas.DEMO_ROL).session(admin).param("rol", "ORGANIZACION"))
                .andExpect(status().isForbidden());
        mvc.perform(post(SALIR).session(admin)).andExpect(status().isForbidden());
        mvc.perform(get(PANEL).session(admin))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(SALIR)));
    }

    @Test
    void niElAdminPuedeReiniciarElDatasetDesdeLaWeb() throws Exception {
        var admin = autenticar(acceso("reinicio"));
        String csrf = csrf(mvc.perform(get(PANEL).session(admin)).andReturn());
        mvc.perform(post(Rutas.DEMO_REINICIAR).session(admin).param("_csrf", csrf))
                .andExpect(status().isForbidden());
    }

    @Test
    void sextoIntentoPorDireccionSeLimitaInclusoConSesionesNuevas() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(ingreso(acceso("limite"), "admin-test", "incorrecta"))
                    .andExpect(redirectedUrl(PANEL + "?error"));
        }
        mvc.perform(ingreso(acceso("limite"), "admin-test", CLAVE))
                .andExpect(status().isTooManyRequests());
        mvc.perform(get(PANEL)).andExpect(status().isOk());
    }

    private Acceso acceso(String direccion) throws Exception {
        var pagina = mvc.perform(get(PANEL)).andExpect(status().isOk()).andReturn();
        return new Acceso((MockHttpSession) pagina.getRequest().getSession(), csrf(pagina), direccion);
    }

    private MockHttpSession autenticar(Acceso acceso) throws Exception {
        var resultado = mvc.perform(ingreso(acceso, "admin-test", CLAVE))
                .andExpect(redirectedUrl(Rutas.RAIZ)).andReturn();
        return (MockHttpSession) resultado.getRequest().getSession();
    }

    private MockHttpServletRequestBuilder ingreso(Acceso acceso, String usuario, String clave) {
        return post(PANEL).session(acceso.sesion()).param("_csrf", acceso.csrf())
                .param("username", usuario).param("password", clave)
                .with(request -> {
                    request.setRemoteAddr(acceso.direccion());
                    return request;
                });
    }

    private static String csrf(MvcResult pagina) throws Exception {
        var matcher = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"")
                .matcher(pagina.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private record Acceso(MockHttpSession sesion, String csrf, String direccion) {
    }
}
