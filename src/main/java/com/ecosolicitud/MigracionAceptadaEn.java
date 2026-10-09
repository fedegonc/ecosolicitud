package com.ecosolicitud;

import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// Completa aceptada_en en solicitudes previas a la columna, con la regla de
// Solicitud.aceptadaSegun: en curso/completada se aceptaron al responder; una
// rechazada se aceptó antes solo si la respuesta precede al cierre.
// Idempotente: solo toca filas con aceptada_en vacía.
@Component
@Order(2)
public class MigracionAceptadaEn implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public MigracionAceptadaEn(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        int filas = jdbc.update("UPDATE solicitudes SET aceptada_en = respondida_en"
                + " WHERE aceptada_en IS NULL AND respondida_en IS NOT NULL"
                + " AND (estado IN ('EN_CURSO', 'COMPLETADA')"
                + " OR (estado = 'RECHAZADA' AND respondida_en < finalizada_en))");
        if (filas > 0) {
            LoggerFactory.getLogger(getClass()).info("aceptada_en completada en {} solicitudes", filas);
        }
    }
}
