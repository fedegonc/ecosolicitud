package com.ecosolicitud;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"ecosolicitud.modo.usuario=", "ecosolicitud.modo.clave-hash="})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ModoSinCredencialesTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void sinVariablesNoHayAdministradorPredeterminado() throws Exception {
        var pagina = mvc.perform(get("/modo/dev")).andExpect(status().isOk()).andReturn();
        var token = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"")
                .matcher(pagina.getResponse().getContentAsString());
        assertThat(token.find()).isTrue();
        mvc.perform(post("/modo/dev")
                        .session((MockHttpSession) pagina.getRequest().getSession())
                        .param("_csrf", token.group(1))
                        .param("username", "admin").param("password", "12345678"))
                .andExpect(redirectedUrl("/modo/dev?error"));
    }
}
