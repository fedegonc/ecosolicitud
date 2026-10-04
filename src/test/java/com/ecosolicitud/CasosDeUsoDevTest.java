package com.ecosolicitud;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"dev", "test"})
@AutoConfigureMockMvc
class CasosDeUsoDevTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void muestraActoresCasosYRelaciones() throws Exception {
        mvc.perform(get("/dev/casos-de-uso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actores.length()").value(2))
                .andExpect(jsonPath("$.casosDeUso.length()").value(15))
                .andExpect(jsonPath("$.relaciones[?(@.tipo == 'include')]", hasSize(5)))
                .andExpect(jsonPath("$.relaciones[?(@.tipo == 'extend')]", hasSize(4)))
                .andExpect(jsonPath("$.relaciones[0].origen").value("CREAR"))
                .andExpect(jsonPath("$.relaciones[0].destino").value("REGISTRAR_AVISO"))
                .andExpect(jsonPath("$.relaciones[5].origen").value("CANCELAR"))
                .andExpect(jsonPath("$.relaciones[5].destino").value("MIS_SOLICITUDES"));
    }
}

@SpringBootTest
@ActiveProfiles({"prod", "test"})
@AutoConfigureMockMvc
class CasosDeUsoProdTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void noExponeElEndpoint() throws Exception {
        mvc.perform(get("/dev/casos-de-uso"))
                .andExpect(status().isNotFound());
    }
}
