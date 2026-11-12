package com.ecosolicitud.cuenta;

import java.util.List;
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

    public Usuario registrarCiudadano(String nombre, String numero, Pais pais, String clave,
            String email) {
        String telefono = validarDatos(nombre, numero, pais, clave);
        var usuario = new Usuario(UUID.randomUUID().toString(), telefono, cifrado.encode(clave),
                Rol.CIUDADANO, null, nombre.strip().toLowerCase(), true);
        var entidad = new UsuarioEntidad(usuario.id(), telefono, usuario.claveHash(),
                usuario.rol(), null, nombre.strip().toLowerCase());
        entidad.cambiarEmail(normalizarEmail(email));
        usuarios.save(entidad);
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

    public static String normalizarEmail(String email) {
        return ValidacionCuenta.normalizarEmail(email);
    }

    // campo → clave de messages, todos a la vez: el formulario los muestra
    // juntos en vez de hacer corregir de a uno por envío
    public Map<String, String> errores(String nombre, String numero, Pais pais, String clave) {
        return errores(nombre, numero, pais, clave, null);
    }

    public Map<String, String> errores(String nombre, String numero, Pais pais, String clave,
            String email) {
        return ValidacionCuenta.errores(usuarios, null, true,
                nombre, numero, pais, clave, email);
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

    // perfil: igual que errores(), pero el propio nombre/celular no cuenta
    // como "ya está en uso" y la contraseña es opcional (vacía = conservar)
    public Map<String, String> erroresDePerfil(String usuarioId, String nombre,
            String numero, Pais pais, String clave) {
        return erroresDePerfil(usuarioId, nombre, numero, pais, clave, null);
    }

    public Map<String, String> erroresDePerfil(String usuarioId, String nombre,
            String numero, Pais pais, String clave, String email) {
        return ValidacionCuenta.errores(usuarios, usuarioId, false,
                nombre, numero, pais, clave, email);
    }

    // guarda los datos del perfil y, si viene una clave nueva, la rota
    public void actualizarPerfil(String usuarioId, String nombre, String numero,
            Pais pais, String clave, String email) {
        var entidad = usuarios.findById(usuarioId).orElseThrow();
        String telefono = Telefono.normalizar(numero, pais);
        entidad.cambiarDatos(nombre.strip().toLowerCase(), telefono, normalizarEmail(email));
        if (clave != null && !clave.isBlank()) {
            entidad.renovarClaveHash(cifrado.encode(clave));
        }
        usuarios.save(entidad);
    }

    public Optional<Usuario> buscar(String numero, Pais pais) {
        try {
            return usuarios.findByTelefono(Telefono.normalizar(numero, pais))
                    .map(u -> new Usuario(u.getId(), u.getTelefono(), u.getClaveHash(),
                            u.getRol(), u.getOrganizacionId(), u.getNombre(), u.isActivo()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // vacío tanto si el número no existe como si la clave no coincide;
    // una cuenta de baja tampoco entra
    public Optional<Usuario> entrar(String numero, Pais pais, String clave) {
        var usuario = buscar(numero, pais).filter(Usuario::activo);
        var hash = usuario.map(Usuario::claveHash).orElse(hashSinCuenta);
        boolean coincide = clave != null && cifrado.matches(clave, hash);
        return coincide ? usuario : Optional.empty();
    }

    // el panel de administración lista todas, incluidas las de baja
    public List<Usuario> usuarios() {
        return usuarios.findAll(org.springframework.data.domain.Sort.by("nombre"))
                .stream()
                .map(u -> new Usuario(u.getId(), u.getTelefono(), u.getClaveHash(),
                        u.getRol(), u.getOrganizacionId(), u.getNombre(), u.isActivo()))
                .toList();
    }

    // alterna el alta de la cuenta desde el panel: no borra nada
    @org.springframework.transaction.annotation.Transactional
    public void cambiarActivo(String id) {
        var u = usuarios.findById(id).orElseThrow();
        u.cambiarActivo(!u.isActivo());
        usuarios.save(u);
    }
}
