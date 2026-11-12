package com.ecosolicitud.cuenta.interno;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Recuperar la clave: un código de 6 dígitos por correo, válido 10 minutos
// y con 5 intentos antes de quemarse. En la base queda solo el hash.
// Sin SMTP configurado el código se loguea y lo muestra la pantalla (dev).
@Slf4j
@Service
public class RecuperacionService {

    static final Duration VIGENCIA = Duration.ofMinutes(10);

    private final CodigoRecuperacionRepository codigos;
    private final UsuariosRepository usuarios;
    private final PasswordEncoder cifrado;
    private final ObjectProvider<JavaMailSender> correo;
    private final String remitente;
    private final SecureRandom azar = new SecureRandom();
    private final Clock reloj = Clock.systemUTC();

    RecuperacionService(CodigoRecuperacionRepository codigos, UsuariosRepository usuarios,
            PasswordEncoder cifrado, ObjectProvider<JavaMailSender> correo,
            @Value("${spring.mail.username:}") String remitente) {
        this.codigos = codigos;
        this.usuarios = usuarios;
        this.cifrado = cifrado;
        this.correo = correo;
        this.remitente = remitente;
    }

    // si el email tiene cuenta genera y manda el código; devuelve el código
    // solo cuando no hay correo configurado (para mostrarlo en dev)
    @Transactional
    public Optional<String> pedirCodigo(String email) {
        var cuenta = usuarios.findByEmail(email);
        if (cuenta.isEmpty()) {
            return Optional.empty();
        }
        var usuario = cuenta.get();
        codigos.findByUsuarioIdAndUsadaFalse(usuario.getId())
                .forEach(CodigoRecuperacion::marcarUsada);
        var codigo = String.format("%06d", azar.nextInt(1_000_000));
        codigos.save(new CodigoRecuperacion(usuario.getId(),
                cifrado.encode(codigo), reloj.instant().plus(VIGENCIA)));
        return enviar(usuario, codigo) ? Optional.empty() : Optional.of(codigo);
    }

    // el código correcto y vigente abre sesión: devuelve la cuenta a entrar
    @Transactional
    public Optional<UsuarioEntidad> verificar(String email, String codigo) {
        var cuenta = usuarios.findByEmail(email);
        if (cuenta.isEmpty()) {
            return Optional.empty();
        }
        var vigente = codigos.findTopByUsuarioIdAndUsadaFalseOrderByCreadaEnDesc(
                cuenta.get().getId()).filter(c -> c.vigente(reloj.instant()));
        if (vigente.isEmpty()) {
            return Optional.empty();
        }
        var c = vigente.get();
        if (!cifrado.matches(codigo == null ? "" : codigo.strip(), c.getCodigoHash())) {
            c.anotarIntento();
            codigos.save(c);
            return Optional.empty();
        }
        c.marcarUsada();
        codigos.save(c);
        return cuenta;
    }

    private boolean enviar(UsuarioEntidad usuario, String codigo) {
        var enviador = correo.getIfAvailable();
        if (enviador == null || remitente.isBlank()) {
            log.warn("sin correo configurado: código de recuperación de {} es {}",
                    usuario.getNombre(), codigo);
            return false;
        }
        var mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(usuario.getEmail());
        mensaje.setSubject("[EcoSolicitud] Tu código para entrar");
        mensaje.setText("Tu código es " + codigo + ".\n"
                + "Vence en " + VIGENCIA.toMinutes() + " minutos.\n"
                + "Si no lo pediste vos, ignorá este correo.");
        try {
            enviador.send(mensaje);
            return true;
        } catch (MailException e) {
            log.warn("no se pudo mandar el código a {}: {}", usuario.getNombre(), e.getMessage());
            return false;
        }
    }
}
