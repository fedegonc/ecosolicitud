package com.ecosolicitud.cuenta.interno;

import java.util.Collection;
import java.util.List;

import com.ecosolicitud.shared.Rol;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UsuarioPrincipal implements UserDetails {

    private final String id;
    private final String nombre;
    private final Rol rol;
    private final String organizacionId;
    private final String telefono;
    private final String claveHash;

    public UsuarioPrincipal(UsuarioEntidad u) {
        this.id = u.getId();
        this.nombre = u.getNombre();
        this.rol = u.getRol();
        this.organizacionId = u.getOrganizacionId();
        this.telefono = u.getTelefono();
        this.claveHash = u.getClaveHash();
    }

    public String getCiudadanoId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Rol getRol() {
        return rol;
    }

    public String getOrganizacionId() {
        return organizacionId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return claveHash;
    }

    @Override
    public String getUsername() {
        return telefono;
    }
}
