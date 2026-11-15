package com.ecosolicitud.solicitud.interno;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.ecosolicitud.solicitud.Estado;
import com.ecosolicitud.solicitud.SolicitudTableroFila;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    Page<Solicitud> findByCiudadano_IdAndEstadoInOrderByCreadaEnDesc(
            String ciudadanoId, Collection<Estado> estados, Pageable pageable);

    Page<Solicitud> findByOrganizacionIdAndEstadoInOrderByCreadaEnDesc(
            String organizacionId, Collection<Estado> estados, Pageable pageable);

    @Query(value = """
            WITH ultimas AS (
                SELECT s.*
                FROM solicitudes s
                WHERE s.organizacion_id = COALESCE(
                        (SELECT pedida.id FROM organizaciones pedida
                         WHERE pedida.id = :organizacionId),
                        (SELECT primera.id FROM organizaciones primera
                         ORDER BY primera.nombre LIMIT 1))
                ORDER BY s.creada_en DESC
                LIMIT :limite
            ), resumen AS (
                SELECT o.nombre AS organizacion_nombre,
                       COUNT(CASE WHEN s.estado = 'PENDIENTE' THEN 1 END) AS total_pendiente,
                       COUNT(CASE WHEN s.estado = 'EN_CURSO' THEN 1 END) AS total_en_curso,
                       COUNT(CASE WHEN s.estado = 'COMPLETADA' THEN 1 END) AS total_completada,
                       COUNT(CASE WHEN s.estado = 'RECHAZADA' THEN 1 END) AS total_rechazada,
                       COUNT(CASE WHEN s.estado = 'CANCELADA' THEN 1 END) AS total_cancelada
                FROM organizaciones o
                LEFT JOIN solicitudes s ON s.organizacion_id = o.id
                WHERE o.id = COALESCE(
                        (SELECT pedida.id FROM organizaciones pedida
                         WHERE pedida.id = :organizacionId),
                        (SELECT primera.id FROM organizaciones primera
                         ORDER BY primera.nombre LIMIT 1))
                GROUP BY o.id, o.nombre
            )
            SELECT u.id AS "id",
                   u.direccion AS "direccion",
                   u.referencia AS "referencia",
                   u.latitud AS "latitud",
                   u.longitud AS "longitud",
                   u.contacto AS "contacto",
                   u.nota AS "nota",
                   u.estado AS "estado",
                   u.creada_en AS "creadaEn",
                   u.respondida_en AS "respondidaEn",
                   u.aceptada_en AS "aceptadaEn",
                   u.finalizada_en AS "finalizadaEn",
                   u.version AS "version",
                   c.nombre AS "ciudadanoNombre",
                   m.id AS "materialId",
                   m.codigo AS "materialCodigo",
                   m.nombre AS "materialNombre",
                   m.icono AS "materialIcono",
                   m.activo AS "materialActivo",
                   r.organizacion_nombre AS "organizacionNombre",
                   r.total_pendiente AS "totalPendiente",
                   r.total_en_curso AS "totalEnCurso",
                   r.total_completada AS "totalCompletada",
                   r.total_rechazada AS "totalRechazada",
                   r.total_cancelada AS "totalCancelada"
            FROM resumen r
            LEFT JOIN ultimas u ON true
            LEFT JOIN ciudadanos c ON c.id = u.ciudadano_id
            LEFT JOIN solicitud_materiales sm ON sm.solicitud_id = u.id
            LEFT JOIN materiales m ON m.id = sm.material_id
            ORDER BY u.creada_en DESC, sm.orden
            """, nativeQuery = true)
    List<SolicitudTableroFila> tablero(@Param("organizacionId") String organizacionId,
            @Param("limite") int limite);

    List<Solicitud> findByCiudadano_IdAndCreadaEnGreaterThanEqualOrderByCreadaEnDesc(
            String ciudadanoId, Instant desde);

    List<Solicitud> findByOrganizacionIdAndCreadaEnGreaterThanEqualOrderByCreadaEnDesc(
            String organizacionId, Instant desde);

    List<Solicitud> findByCiudadano_IdAndCreadaEnGreaterThanEqualAndCreadaEnLessThanOrderByCreadaEnDesc(
            String ciudadanoId, Instant desde, Instant hasta);

    List<Solicitud> findByOrganizacionIdAndCreadaEnGreaterThanEqualAndCreadaEnLessThanOrderByCreadaEnDesc(
            String organizacionId, Instant desde, Instant hasta);

    Optional<Solicitud> findTop1ByCiudadano_IdOrderByCreadaEnDesc(String ciudadanoId);

    List<Solicitud> findByCreadaEnGreaterThanEqual(Instant desde);
}
