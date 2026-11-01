package com.ecosolicitud.cuenta;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.ecosolicitud.shared.Rol;

import com.ecosolicitud.cuenta.interno.UsuarioEntidad;
import com.ecosolicitud.cuenta.interno.UsuariosRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Registro y entrada por celular. Java puro: el repositorio y el cifrado
// llegan por constructor (en Spring, JPA y BCryptPasswordEncoder).
@Service
public class Cuentas {

    public static final int CLAVE_MINIMA = 5;
    // BCrypt solo usa los primeros 72 bytes
    public static final int CLAVE_MAXIMA_BYTES = 72;
    // nombre de usuario: mínimo 5, sin espacios, se guarda en minúsculas
    private static final int NOMBRE_MINIMO = 5;
    private static final java.util.regex.Pattern NOMBRE_FORMATO =
            java.util.regex.Pattern.compile("[a-zA-Z0-9_.\\-]+");

    private final UsuariosRepository usuarios;
    private final PasswordEncoder cifrado;
    // con un número inexistente se compara igual contra un hash: el tiempo
    // de respuesta no delata qué números tienen cuenta
    private final String hashSinCuenta;

    public Cuentas(UsuariosRepository usuarios, PasswordEncoder cifrado) {
        this.usuarios = usuarios;
        this.cifrado = cifrado;
        this.hashSinCuenta = cifrado.encode(UUID.randomUUID().toString());
    }

    public Usuario registrarCiudadano(String nombre, String numero, Pais pais, String clave) {
        String telefono = validarDatos(nombre, numero, pais, clave);
        var usuario = new Usuario(UUID.randomUUID().toString(), telefono, cifrado.encode(clave),
                Rol.CIUDADANO, null);
        usuarios.save(new UsuarioEntidad(usuario.id(), telefono, usuario.claveHash(),
                usuario.rol(), null, nombre.strip().toLowerCase()));
        return usuario;
    }

    // Valida sin persistir y corta en el primer error (para el alta)
    public String validarDatos(String nombre, String numero, Pais pais, String clave) {
        var errores = errores(nombre, numero, pais, clave);
        if (!errores.isEmpty()) {
            var primero = errores.entrySet().iterator().next();
            throw new CuentaException(primero.getKey(), primero.getValue());
        }
        return Telefono.normalizar(numero, pais);
    }

    // campo → clave de messages, todos a la vez: el formulario los muestra
    // juntos en vez de hacer corregir de a uno por envío
    public Map<String, String> errores(String nombre, String numero, Pais pais, String clave) {
        Map<String, String> errores = new LinkedHashMap<>();
        var usuario = nombre == null ? "" : nombre.strip().toLowerCase();
        if (usuario.length() < NOMBRE_MINIMO) {
            errores.put("nombre", "cuenta.error.nombre.corto");
        } else if (!NOMBRE_FORMATO.matcher(usuario).matches()) {
            errores.put("nombre", "cuenta.error.nombre.formato");
        } else if (usuarios.findByNombre(usuario).isPresent()) {
            errores.put("nombre", "cuenta.error.nombre.usado");
        }
        try {
            if (usuarios.findByTelefono(Telefono.normalizar(numero, pais)).isPresent()) {
                errores.put("telefono", "cuenta.error.telefono.usado");
            }
        } catch (IllegalArgumentException e) {
            errores.put("telefono", "cuenta.error.telefono");
        }
        if (clave == null || clave.length() < CLAVE_MINIMA
                || clave.getBytes(StandardCharsets.UTF_8).length > CLAVE_MAXIMA_BYTES) {
            errores.put("clave", "cuenta.error.clave");
        }
        return errores;
    }

    // el registro repetido por la misma persona (la respuesta del primero se
    // perdió): nombre, celular y contraseña de un mismo vecino. Pedir los tres
    // evita que el registro sirva para probar claves sin el límite del login
    public boolean esLaMismaCuenta(String nombre, String numero, Pais pais, String clave) {
        if (nombre == null || clave == null || pais == null) {
            return false;
        }
        String telefono;
        try {
            telefono = Telefono.normalizar(numero, pais);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return usuarios.findByNombre(nombre.strip().toLowerCase())
                .filter(u -> u.getRol() == Rol.CIUDADANO && telefono.equals(u.getTelefono()))
                .map(u -> cifrado.matches(clave, u.getClaveHash()))
                .orElse(false);
    }

    // el celular de la cuenta, listo para precargar un campo de contacto
    public Optional<String> telefonoDe(String usuarioId) {
        return usuarioId == null ? Optional.empty()
                : usuarios.findById(usuarioId)
                        .map(u -> Telefono.presentar(u.getTelefono()));
    }

    public Optional<Usuario> buscar(String numero, Pais pais) {
        try {
            return usuarios.findByTelefono(Telefono.normalizar(numero, pais))
                    .map(u -> new Usuario(u.getId(), u.getTelefono(), u.getClaveHash(),
                            u.getRol(), u.getOrganizacionId()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // vacío tanto si el número no existe como si la clave no coincide
    public Optional<Usuario> entrar(String numero, Pais pais, String clave) {
        var usuario = buscar(numero, pais);
        var hash = usuario.map(Usuario::claveHash).orElse(hashSinCuenta);
        boolean coincide = clave != null && cifrado.matches(clave, hash);
        return coincide ? usuario : Optional.empty();
    }
}
