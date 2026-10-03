package com.ecosolicitud.organizacion;

import java.util.List;

import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;

public record OrganizacionInfo(String id, String nombre, Ciudad ciudad,
        List<Material> materiales, String horario, String telefono, long version) {
}
