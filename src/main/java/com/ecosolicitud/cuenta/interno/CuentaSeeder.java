package com.ecosolicitud.cuenta.interno;

import java.util.List;
import java.util.UUID;

import com.ecosolicitud.shared.Rol;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Las cuentas de los centros se crean a mano por ahora: una por ciudad. No hay
// registro público de organizaciones todavía; la clave sale de una env var.
@Component
@Order(5)
public class CuentaSeeder implements ApplicationRunner {

    private record Centro(String usuario, String organizacionId) {
    }

    private static final List<Centro> CENTROS = List.of(
            new Centro("renacer-norte", "frontera-limpia"),
            new Centro("novo-horizonte", "coleta-solidaria"));

    private final UsuariosRepository usuarios;
    private final PasswordEncoder cifrado;
    private final String clave;

    CuentaSeeder(UsuariosRepository usuarios, PasswordEncoder cifrado,
            @Value("${ecosolicitud.cuentas.clave-centros:centro-demo}") String clave) {
        this.usuarios = usuarios;
        this.cifrado = cifrado;
        this.clave = clave;
    }

    @Override
    public void run(ApplicationArguments args) {
        int n = 0;
        for (Centro c : CENTROS) {
            if (usuarios.findByNombre(c.usuario()).isPresent()) {
                continue;
            }
            usuarios.save(new UsuarioEntidad(UUID.randomUUID().toString(),
                    "59800000" + n, cifrado.encode(clave), Rol.ORGANIZACION,
                    c.organizacionId(), c.usuario()));
            n++;
        }
    }
}
