package com.ecosolicitud.cuenta.interno;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CuentaUserDetails implements UserDetailsService {

    private final UsuariosRepository usuarios;
    private final String usuario;
    private final String claveGuardada;
    private final boolean habilitado;

    public CuentaUserDetails(UsuariosRepository usuarios,
            @Value("${ecosolicitud.modo.usuario:}") String usuario,
            @Value("${ecosolicitud.modo.clave:}") String clave,
            @Value("${ecosolicitud.modo.clave-hash:}") String hash,
            @Value("${ecosolicitud.demo.habilitada:true}") boolean habilitado,
            PasswordEncoder cifrado) {
        this.usuarios = usuarios;
        this.usuario = usuario;
        this.habilitado = habilitado;
        this.claveGuardada = hash.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")
                ? hash
                : clave.isBlank() ? null : cifrado.encode(clave);
    }

    @Override
    public UserDetails loadUserByUsername(String entrada) throws UsernameNotFoundException {
        // el admin entra siempre: ROLE_ADMIN habilita /admin aunque la
        // demostración esté apagada; ROLE_DEMO solo cuando está encendida
        if (!usuario.isBlank() && usuario.equals(entrada) && claveGuardada != null) {
            var permisos = new java.util.ArrayList<SimpleGrantedAuthority>();
            permisos.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            if (habilitado) {
                permisos.add(new SimpleGrantedAuthority("ROLE_DEMO"));
            }
            return new User(usuario, claveGuardada, permisos);
        }
        // una cuenta de baja no entra: mismo "sin cuenta" genérico
        return usuarios.findByNombre(entrada == null ? "" : entrada.strip().toLowerCase())
                .filter(UsuarioEntidad::isActivo)
                .map(UsuarioPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("sin cuenta"));
    }
}
