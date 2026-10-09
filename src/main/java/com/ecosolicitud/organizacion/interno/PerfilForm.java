package com.ecosolicitud.organizacion.interno;

import java.util.ArrayList;
import java.util.List;

import com.ecosolicitud.organizacion.OrganizacionInfo;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.shared.Ubicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerfilForm {

    @NotEmpty(message = "{perfil.error.materiales}")
    private List<Material> materiales = new ArrayList<>();

    @NotBlank(message = "{perfil.error.horario}")
    @Size(max = 80, message = "{perfil.error.horario}")
    private String horario;

    @NotBlank(message = "{perfil.error.telefono}")
    @Size(max = 30, message = "{perfil.error.telefono}")
    @Pattern(regexp = Organizacion.PATRON_TELEFONO,
            message = "{perfil.error.telefono}")
    private String telefono;

    private Double latitud;
    private Double longitud;

    private long version;

    public Ubicacion getUbicacion() {
        if (latitud == null || longitud == null) {
            return null;
        }
        try {
            return new Ubicacion(latitud, longitud);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static PerfilForm de(OrganizacionInfo org) {
        var form = new PerfilForm();
        form.setMateriales(new ArrayList<>(org.materiales()));
        form.setHorario(org.horario());
        form.setTelefono(org.telefono());
        if (org.ubicacion() != null) {
            form.setLatitud(org.ubicacion().latitud());
            form.setLongitud(org.ubicacion().longitud());
        }
        form.setVersion(org.version());
        return form;
    }
}
