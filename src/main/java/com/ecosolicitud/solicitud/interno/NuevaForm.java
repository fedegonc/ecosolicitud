package com.ecosolicitud.solicitud.interno;

import java.util.ArrayList;
import java.util.List;

import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;

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
    @Size(max = 80, message = "{nueva.error.nombre}")
    private String nombre;
    @NotBlank(message = "{nueva.error.contacto}")
    @Size(max = 40, message = "{nueva.error.contacto}")
    private String contacto;
    @NotBlank(message = "{nueva.error.direccion}")
    @Size(max = 120, message = "{nueva.error.direccion}")
    private String direccion;
    @Size(max = 120, message = "{nueva.error.referencia}")
    private String referencia;
    @Size(max = 300, message = "{nueva.error.nota}")
    private String nota;
}
