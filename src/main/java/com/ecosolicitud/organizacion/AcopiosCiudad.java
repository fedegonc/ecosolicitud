package com.ecosolicitud.organizacion;

import java.util.List;

import com.ecosolicitud.shared.Ciudad;

public record AcopiosCiudad(Ciudad ciudad, List<OrganizacionInfo> organizaciones) {
}
