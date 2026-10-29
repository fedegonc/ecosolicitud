package com.ecosolicitud.cuenta;

import java.nio.charset.StandardCharsets;
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

    // Valida sin persistir: el formulario junta todos los errores antes de crear
    public String validarDatos(String nombre, String numero, Pais pais, String clave) {
        if (nombre == null || nombre.strip().length() < NOMBRE_MINIMO) {
            throw new CuentaException("nombre", "El nombre de usuario necesita al menos 5 caracteres");
        }
        var usuario = nombre.strip().toLowerCase();
        if (!NOMBRE_FORMATO.matcher(usuario).matches()) {
            throw new CuentaException("nombre",
                    "El nombre de usuario no admite espacios ni símbolos");
        }
        String telefono;
        try {
            telefono = Telefono.normalizar(numero, pais);
        } catch (IllegalArgumentException e) {
            throw new CuentaException("telefono", "Ingresá un celular de Uruguay o Brasil");
        }
        if (clave == null || clave.length() < CLAVE_MINIMA
                || clave.getBytes(StandardCharsets.UTF_8).length > CLAVE_MAXIMA_BYTES) {
            throw new CuentaException("clave", "La contraseña necesita al menos 5 caracteres");
        }
        if (usuarios.findByNombre(usuario).isPresent()) {
            throw new CuentaException("nombre", "Ese nombre de usuario ya está en uso");
        }
        if (usuarios.findByTelefono(telefono).isPresent()) {
            throw new CuentaException("telefono", "Ese celular ya tiene una cuenta");
        }
        return telefono;
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
