package com.ecosolicitud;

import com.ecosolicitud.shared.CatalogoMateriales;

import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// Migra las colecciones de enum (columna `material` con el nombre del enum)
// a la FK `material_id` del catálogo. Corre antes del seed de demo (@Order 1).
// Es idempotente: si las columnas legacy no existen no hace nada.
@Component
@Order(1)
public class MigracionMateriales implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final CatalogoMateriales catalogo;

    public MigracionMateriales(JdbcTemplate jdbc, CatalogoMateriales catalogo) {
        this.jdbc = jdbc;
        this.catalogo = catalogo;
    }

    @Override
    public void run(ApplicationArguments args) {
        catalogo.semillarSiVacio();
        migrarAsociacion("solicitud_materiales");
        migrarAsociacion("organizacion_materiales");
        migrarArticulos();
    }

    private void migrarAsociacion(String tabla) {
        if (!existeColumna(tabla, "material")) {
            return;
        }
        // hibernate update no agrega columnas a join tables existentes: la
        // creamos acá antes del backfill
        if (!existeColumna(tabla, "material_id")) {
            jdbc.execute("ALTER TABLE " + tabla + " ADD COLUMN material_id BIGINT");
        }
        int pendientes = jdbc.update("UPDATE " + tabla + " t SET material_id = "
                + "(SELECT id FROM materiales WHERE codigo = CAST(t.material AS VARCHAR))");
        int huerfanas = jdbc.update("DELETE FROM " + tabla
                + " WHERE material_id IS NULL");
        if (huerfanas > 0) {
            LoggerFactory.getLogger(getClass()).warn(
                    "{} filas de {} con material fuera del catálogo descartadas",
                    huerfanas, tabla);
        }
        jdbc.execute("ALTER TABLE " + tabla + " DROP COLUMN material");
        jdbc.execute("ALTER TABLE " + tabla
                + " ALTER COLUMN material_id SET NOT NULL");
        LoggerFactory.getLogger(getClass()).info(
                "{} migrada a material_id ({} filas)", tabla, pendientes);
    }

    private void migrarArticulos() {
        if (!existeColumna("articulos", "material")) {
            return;
        }
        if (!existeColumna("articulos", "material_id")) {
            jdbc.execute("ALTER TABLE articulos ADD COLUMN material_id BIGINT");
        }
        int resueltos = jdbc.update("UPDATE articulos a SET material_id = "
                + "(SELECT id FROM materiales WHERE codigo = CAST(a.material AS VARCHAR))");
        Integer sinResolver = jdbc.queryForObject(
                "SELECT COUNT(*) FROM articulos WHERE material IS NOT NULL"
                        + " AND material_id IS NULL", Integer.class);
        if (sinResolver != null && sinResolver > 0) {
            LoggerFactory.getLogger(getClass()).warn(
                    "{} articulos con material fuera del catálogo quedan generales",
                    sinResolver);
        }
        jdbc.execute("ALTER TABLE articulos DROP COLUMN material");
        LoggerFactory.getLogger(getClass()).info(
                "articulos migrada a material_id ({} filas)", resueltos);
    }

    private boolean existeColumna(String tabla, String columna) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE UPPER(TABLE_NAME) = ? AND UPPER(COLUMN_NAME) = ?",
                Integer.class, tabla.toUpperCase(), columna.toUpperCase());
        return n != null && n > 0;
    }
}
