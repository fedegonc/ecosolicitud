package com.ecosolicitud.cuenta;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ecosolicitud.cuenta.interno.UsuariosRepository;
import com.ecosolicitud.shared.Limites;

// Validación de campos compartida por registro y perfil: campo → clave de
// messages, todos a la vez (el formulario los muestra juntos en vez de
// hacer corregir de a uno por envío)
final class ValidacionCuenta {

    // nombre de usuario: mínimo 5, sin espacios, se guarda en minúsculas
    private static final int NOMBRE_MINIMO = 5;
    private static final java.util.regex.Pattern NOMBRE_FORMATO =
            java.util.regex.Pattern.compile("[a-zA-Z0-9_.\\-]+");
    private static final java.util.regex.Pattern EMAIL_FORMATO =
            java.util.regex.Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private ValidacionCuenta() {
    }

    static String normalizarEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.strip().toLowerCase();
    }

    // usuarioId evita marcarse "en uso" a sí mismo (perfil); con
    // claveObligatoria una clave vacía es error, sin ella "vacía = conservar"
    static Map<String, String> errores(UsuariosRepository usuarios, String usuarioId,
            boolean claveObligatoria, String nombre, String numero, Pais pais,
            String clave, String email) {
        Map<String, String> errores = new LinkedHashMap<>();
        var usuario = nombre == null ? "" : nombre.strip().toLowerCase();
        if (usuario.length() < NOMBRE_MINIMO) {
            errores.put("nombre", "cuenta.error.nombre.corto");
        } else if (!NOMBRE_FORMATO.matcher(usuario).matches()) {
            errores.put("nombre", "cuenta.error.nombre.formato");
        } else if (usuarios.findByNombre(usuario)
                .filter(u -> !u.getId().equals(usuarioId)).isPresent()) {
            errores.put("nombre", "cuenta.error.nombre.usado");
        }
        try {
            if (usuarios.findByTelefono(Telefono.normalizar(numero, pais))
                    .filter(u -> !u.getId().equals(usuarioId)).isPresent()) {
                errores.put("telefono", "cuenta.error.telefono.usado");
            }
        } catch (IllegalArgumentException e) {
            errores.put("telefono", "cuenta.error.telefono");
        }
        boolean vacia = clave == null || clave.isBlank();
        boolean invalida = clave != null && (clave.length() < Cuentas.CLAVE_MINIMA
                || clave.getBytes(StandardCharsets.UTF_8).length > Cuentas.CLAVE_MAXIMA_BYTES);
        if ((vacia && claveObligatoria) || (!vacia && invalida)) {
            errores.put("clave", "cuenta.error.clave");
        }
        errorDeEmail(usuarios, email, usuarioId, errores);
        return errores;
    }

    private static void errorDeEmail(UsuariosRepository usuarios, String email,
            String usuarioId, Map<String, String> errores) {
        var normalizado = normalizarEmail(email);
        if (normalizado == null) {
            return;
        }
        if (normalizado.length() > Limites.EMAIL
                || !EMAIL_FORMATO.matcher(normalizado).matches()) {
            errores.put("email", "cuenta.error.email");
        } else if (usuarios.findByEmail(normalizado)
                .filter(u -> !u.getId().equals(usuarioId)).isPresent()) {
            errores.put("email", "cuenta.error.email.usado");
        }
    }
}
