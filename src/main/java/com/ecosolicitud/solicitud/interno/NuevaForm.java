package com.ecosolicitud.solicitud.interno;

import java.util.ArrayList;
import java.util.List;

import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Limites;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.shared.Ubicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NuevaForm {

    @NotNull(message = "{nueva.error.ciudad}")
    private Ciudad ciudad;
    @NotEmpty(message = "{nueva.error.materiales}")
    private List<Material> materiales = new ArrayList<>();
    @NotBlank(message = "{nueva.error.organizacion}")
    private String organizacionId;
    @NotBlank(message = "{nueva.error.nombre}")
    @Size(max = Limites.NOMBRE, message = "{error.largo}")
    private String nombre;
    @NotBlank(message = "{nueva.error.contacto}")
    @Size(max = Limites.CONTACTO, message = "{error.largo}")
    private String contacto;
    @NotBlank(message = "{nueva.error.direccion}")
    @Size(max = Limites.DIRECCION, message = "{error.largo}")
    private String direccion;
    @Size(max = Limites.REFERENCIA, message = "{error.largo}")
    private String referencia;
    private Double latitud;
    private Double longitud;
    @Size(max = Limites.NOTA, message = "{error.largo}")
    private String nota;

    // Thymeleaf la usa como ${form.ubicacion}: solo arma el valor si el par
    // vino completo y en rango — un input tocado a mano no rompe el render.
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
}
