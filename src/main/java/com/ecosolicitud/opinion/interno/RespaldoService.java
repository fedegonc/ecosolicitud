package com.ecosolicitud.opinion.interno;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.ecosolicitud.shared.Rol;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

// Guarda primero y después envía: si el correo falla, la respuesta no se pierde.
@Slf4j
@Service
class RespaldoService {

    private final RespuestaRespaldoRepository repositorio;
    private final ObjectProvider<JavaMailSender> correo;
    private final String destino;
    private final String remitente;
    private final Clock reloj = Clock.systemUTC();

    RespaldoService(RespuestaRespaldoRepository repositorio, ObjectProvider<JavaMailSender> correo,
            @Value("${ecosolicitud.cuestionario.destino}") String destino,
            @Value("${spring.mail.username:}") String remitente) {
        this.repositorio = repositorio;
        this.correo = correo;
        this.destino = destino;
        this.remitente = remitente;
    }

    void registrar(Rol rol, String idioma, Cuestionario cuestionario,
            List<Cuestionario.Respuesta> respuestas) {
        Instant ahora = Instant.now(reloj);
        String contenido = texto(cuestionario, respuestas, ahora);
        var respuesta = repositorio.save(new RespuestaRespaldo(rol, idioma, contenido, ahora));
        var enviador = correo.getIfAvailable();
        if (enviador == null || remitente.isBlank() || destino.isBlank()) {
            log.warn("Respuesta de respaldo {} guardada sin enviar: no hay correo configurado", respuesta.getId());
            return;
        }
        var mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destino);
        mensaje.setSubject("[EcoSolicitud] Respuesta de respaldo #" + respuesta.getId()
                + " · " + cuestionario.titulo() + " (" + idioma.toUpperCase() + ")");
        mensaje.setText(contenido);
        try {
            enviador.send(mensaje);
            respuesta.marcarEnviadaPorCorreo();
            repositorio.save(respuesta);
        } catch (MailException e) {
            log.warn("Respuesta de respaldo {} guardada sin enviar: {}", respuesta.getId(), e.getMessage());
        }
    }

    private static String texto(Cuestionario cuestionario, List<Cuestionario.Respuesta> respuestas,
            Instant ahora) {
        var sb = new StringBuilder(cuestionario.titulo()).append('\n')
                .append("Respondido: ").append(ahora).append(" (UTC)\n\n");
        for (int i = 0; i < respuestas.size(); i++) {
            var r = respuestas.get(i);
            sb.append(i + 1).append(". ").append(r.pregunta()).append('\n')
                    .append("   → ").append(r.valor()).append('\n');
        }
        return sb.toString();
    }
}
