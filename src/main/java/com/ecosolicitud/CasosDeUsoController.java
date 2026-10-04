package com.ecosolicitud;

import java.util.List;

import com.ecosolicitud.shared.Rol;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev & !prod")
class CasosDeUsoController {

    private static final List<Rol> CIUDADANO = List.of(Rol.CIUDADANO);
    private static final List<Rol> ORGANIZACION = List.of(Rol.ORGANIZACION);
    private static final List<Rol> AMBOS = List.of(Rol.CIUDADANO, Rol.ORGANIZACION);

    @GetMapping("/dev/casos-de-uso")
    Catalogo casosDeUso() {
        return new Catalogo(
                List.of(new ActorInfo(Rol.CIUDADANO, "Ciudadano"),
                        new ActorInfo(Rol.ORGANIZACION, "Representante del centro de acopio")),
                List.of(
                        new CasoDeUso("CREAR", "Crear solicitud de retiro", CIUDADANO),
                        new CasoDeUso("MIS_SOLICITUDES", "Consultar mis solicitudes y su estado", CIUDADANO),
                        new CasoDeUso("CANCELAR", "Cancelar solicitud pendiente", CIUDADANO),
                        new CasoDeUso("CENTROS", "Consultar centros de acopio", CIUDADANO),
                        new CasoDeUso("RECIBIDAS", "Consultar solicitudes recibidas", ORGANIZACION),
                        new CasoDeUso("ACEPTAR", "Aceptar solicitud pendiente", ORGANIZACION),
                        new CasoDeUso("RECHAZAR", "Rechazar solicitud pendiente o en curso", ORGANIZACION),
                        new CasoDeUso("COMPLETAR", "Marcar retiro como completado", ORGANIZACION),
                        new CasoDeUso("PERFIL", "Editar perfil del acopio", ORGANIZACION),
                        new CasoDeUso("AVISOS", "Consultar avisos", AMBOS),
                        new CasoDeUso("GUIA", "Consultar guía de reciclaje", AMBOS),
                        new CasoDeUso("COMUNIDAD", "Consultar publicaciones de comunidad", AMBOS),
                        new CasoDeUso("ESTADISTICAS", "Consultar estadísticas", AMBOS),
                        new CasoDeUso("OPINION", "Valorar funciones del sistema", AMBOS),
                        new CasoDeUso("REGISTRAR_AVISO", "Registrar aviso al destinatario", List.of())),
                List.of(
                        new Relacion("include", "CREAR", "REGISTRAR_AVISO", "Siempre al crear"),
                        new Relacion("include", "CANCELAR", "REGISTRAR_AVISO", "Siempre al cancelar"),
                        new Relacion("include", "ACEPTAR", "REGISTRAR_AVISO", "Siempre al aceptar"),
                        new Relacion("include", "RECHAZAR", "REGISTRAR_AVISO", "Siempre al rechazar"),
                        new Relacion("include", "COMPLETAR", "REGISTRAR_AVISO", "Siempre al completar"),
                        new Relacion("extend", "CANCELAR", "MIS_SOLICITUDES", "Solicitud propia pendiente"),
                        new Relacion("extend", "ACEPTAR", "RECIBIDAS", "Solicitud destinataria pendiente"),
                        new Relacion("extend", "RECHAZAR", "RECIBIDAS", "Solicitud destinataria pendiente o en curso"),
                        new Relacion("extend", "COMPLETAR", "RECIBIDAS", "Solicitud destinataria en curso")));
    }

    record Catalogo(List<ActorInfo> actores, List<CasoDeUso> casosDeUso, List<Relacion> relaciones) {
    }

    record ActorInfo(Rol id, String nombre) {
    }

    record CasoDeUso(String id, String nombre, List<Rol> actores) {
    }

    record Relacion(String tipo, String origen, String destino, String condicion) {
    }
}
