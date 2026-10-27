package com.ecosolicitud;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.ecosolicitud.demo.DemoService;
import com.ecosolicitud.shared.Rutas;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

// Presupuesto de consultas SQL por página. Cada consulta es un viaje a la base:
// con la base remota (Supabase) 19 consultas eran ~3,5 s solo de espera. Un
// presupuesto que sube hace fallar el test; cuando una mejora lo baja, se
// ajusta el número acá.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Epic("Rendimiento")
@Feature("Consultas por página")
class ConsultasPorPaginaTest {

    private static final Map<String, Long> CIUDADANO = presupuesto(
            Rutas.RAIZ, 10,
            Rutas.MIS_SOLICITUDES, 8,
            Rutas.NUEVA, 1,
            Rutas.NUEVA + "?ciudad=RIVERA&organizacionId=frontera-limpia", 3,
            Rutas.ACOPIOS, 3,
            Rutas.AVISOS, 11,
            Rutas.GUIA, 3,
            Rutas.COMUNIDAD, 3,
            Rutas.ESTADISTICAS, 6,
            Rutas.OPINION, 1);

    private static final Map<String, Long> ORGANIZACION = presupuesto(
            Rutas.ORG_SOLICITUDES, 16,
            Rutas.ORG_PERFIL, 12,
            Rutas.AVISOS, 14);

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private DemoService demo;

    @Autowired
    private EntityManagerFactory emf;

    private Statistics estadisticas;

    @BeforeEach
    void setUp() {
        demo.reiniciar();
        estadisticas = emf.unwrap(SessionFactory.class).getStatistics();
    }

    @Test
    @DisplayName("Ciudadano: ninguna página supera su presupuesto de consultas")
    void ciudadano() {
        medir("ciudadano", new Sesion(), CIUDADANO);
    }

    @Test
    @DisplayName("Estadísticas: la segunda visita del día sale de la caché")
    void estadisticasDelDia() {
        var sesion = new Sesion();
        sesion.get(Rutas.ESTADISTICAS);
        medir("caché", sesion, presupuesto(Rutas.ESTADISTICAS, 1));
    }

    @Test
    @DisplayName("Organización: ninguna página supera su presupuesto de consultas")
    void organizacion() {
        var sesion = new Sesion();
        sesion.post(Rutas.DEMO_ROL, "rol", "ORGANIZACION");
        sesion.post(Rutas.DEMO_ORGANIZACION, "id", "frontera-limpia");
        medir("organización", sesion, ORGANIZACION);
    }

    private void medir(String rol, Sesion sesion, Map<String, Long> presupuesto) {
        var medidas = new LinkedHashMap<String, Long>();
        presupuesto.keySet().forEach(ruta -> {
            estadisticas.clear();
            assertThat(sesion.get(ruta)).as(ruta).isEqualTo(HttpStatus.OK);
            medidas.put(ruta, estadisticas.getPrepareStatementCount());
        });
        medidas.forEach((ruta, n) -> System.out.printf("consultas %-13s %3d / %3d  %s%n",
                rol, n, presupuesto.get(ruta), ruta));
        medidas.forEach((ruta, n) -> assertThat(n)
                .as("consultas de %s (%s)", ruta, rol).isLessThanOrEqualTo(presupuesto.get(ruta)));
    }

    private static Map<String, Long> presupuesto(Object... rutaYMaximo) {
        var m = new LinkedHashMap<String, Long>();
        for (int i = 0; i < rutaYMaximo.length; i += 2) {
            m.put((String) rutaYMaximo[i], ((Integer) rutaYMaximo[i + 1]).longValue());
        }
        return m;
    }

    private class Sesion {
        private final String cookie;
        private final String csrf;

        Sesion() {
            var pagina = rest.getForEntity(Rutas.ACOPIOS, String.class);
            cookie = pagina.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
            var m = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"").matcher(pagina.getBody());
            assertThat(m.find()).isTrue();
            csrf = m.group(1);
        }

        HttpStatus get(String ruta) {
            var h = new HttpHeaders();
            h.add(HttpHeaders.COOKIE, cookie);
            var r = rest.exchange(ruta, HttpMethod.GET, new HttpEntity<>(h), String.class);
            return HttpStatus.valueOf(r.getStatusCode().value());
        }

        void post(String ruta, String campo, String valor) {
            var body = new LinkedMultiValueMap<String, String>();
            body.add(campo, valor);
            body.add("_csrf", csrf);
            var h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            h.add(HttpHeaders.COOKIE, cookie);
            rest.postForEntity(ruta, new HttpEntity<>(body, h), String.class);
        }
    }
}
