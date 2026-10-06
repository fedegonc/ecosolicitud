package com.ecosolicitud;

import com.ecosolicitud.shared.Actor;
import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.solicitud.Estado;
import com.ecosolicitud.solicitud.SolicitudService;
import com.ecosolicitud.solicitud.interno.Solicitud;
import com.ecosolicitud.solicitud.interno.SolicitudRepository;

import jakarta.persistence.EntityManager;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Epic("Regresión: conflicto de versión detectado en el flush, no en el pre-check")
@ActiveProfiles("test")
@SpringBootTest
class SolicitudConcurrenciaTest {

    @Autowired
    private SolicitudService servicio;
    @Autowired
    private SolicitudRepository repositorio;
    @Autowired
    private EntityManager em;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager txManager;

    @Test
    @Transactional
    void versionQueCambiaDuranteLaTransacionPropagaOptimisticLocking() {
        Solicitud pendiente = repositorio.findAll().stream()
                .filter(s -> s.getEstado() == Estado.PENDIENTE).findFirst().orElseThrow();
        long id = pendiente.getId();

        // La TX del test cachea la entidad con su versión original...
        em.find(Solicitud.class, id);
        long versionVista = pendiente.getVersion();

        // ...y otra TX la modifica y commitea (el "ganador" de la carrera).
        var otraTx = new TransactionTemplate(txManager);
        otraTx.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
        otraTx.executeWithoutResult(s ->
                jdbc.update("UPDATE solicitudes SET version = version + 1 WHERE id = ?", id));

        // findById devuelve la copia cacheada: el pre-check pasa y el UPDATE falla.
        var actor = new Actor(Rol.ORGANIZACION, null, null, pendiente.getOrganizacionId());
        assertThatThrownBy(() -> servicio.aceptar(actor, id, versionVista))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }
}
