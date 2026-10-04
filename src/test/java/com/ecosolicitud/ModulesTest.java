package com.ecosolicitud;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulesTest {

    @Test
    void moduleBoundariesAreValid() {
        ApplicationModules.of(EcoSolicitudApplication.class).verify();
    }
}
