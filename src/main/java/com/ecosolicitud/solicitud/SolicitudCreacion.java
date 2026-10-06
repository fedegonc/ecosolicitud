package com.ecosolicitud.solicitud;

import java.util.List;

import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;

// Contrato de entrada del service: valida todo, igual que NuevaForm en la web.
// Si falla acá es un bug del caller, no un error de usuario.
public record SolicitudCreacion(
    Ciudad ciudad,
    String direccion,
    String referencia,
    List<Material> materiales,
    String organizacionId,
    String nombre,
    String contacto,
    String nota
) {
    public SolicitudCreacion {
        if (ciudad == null
                || direccion == null || direccion.isBlank() || direccion.length() > 120
                || referencia != null && referencia.length() > 120
                || materiales == null || materiales.isEmpty()
                || organizacionId == null || organizacionId.isBlank()
                || nombre == null || nombre.isBlank() || nombre.length() > 80
                || contacto == null || contacto.isBlank() || contacto.length() > 40
                || nota != null && nota.length() > 300) {
            throw new IllegalArgumentException("SolicitudCreacion inválida");
        }
        materiales = List.copyOf(materiales);
    }
}
