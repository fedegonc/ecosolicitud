package com.ecosolicitud.organizacion;

import java.util.List;

import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;

public record OrganizacionInfo(String id, String nombre, Ciudad ciudad,
        List<Material> materiales, String horario, String telefono, long version) {

    // ¿El centro acepta todos los pedidos? Solo cuentan los activos: una
    // solicitud nueva no puede pedir un material dado de baja aunque el centro
    // lo tenga asociado históricamente. La usa el service (RN-04) y el
    // controller para el error por campo — una sola fuente de la regla.
    public boolean recibeTodos(java.util.Collection<Material> pedidos) {
        return materiales.stream().filter(Material::isActivo)
                .collect(java.util.stream.Collectors.toSet())
                .containsAll(pedidos);
    }
}
