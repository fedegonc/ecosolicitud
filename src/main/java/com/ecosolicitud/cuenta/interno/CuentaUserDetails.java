package com.ecosolicitud.cuenta.interno;

import java.util.List;



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
        if (habilitado && !usuario.isBlank() && usuario.equals(entrada) && claveGuardada != null) {
            return new User(usuario, claveGuardada,
                    List.of(new SimpleGrantedAuthority("ROLE_DEMO")));
        }
        return usuarios.findByNombre(entrada == null ? "" : entrada.strip().toLowerCase())
                .map(UsuarioPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("sin cuenta"));
    }
}
