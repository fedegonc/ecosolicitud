package com.ecosolicitud.solicitud;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.ecosolicitud.shared.Fechas;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.shared.Ubicacion;
import com.ecosolicitud.solicitud.interno.Solicitud;

public record SolicitudInfo(long id, String direccion, String referencia,
        Ubicacion ubicacion, List<Material> materiales, String organizacionNombre,
        String nombreCiudadano, String contacto, String nota, Estado estado,
        Instant creadaEn, Instant finalizadaEn, long version) {

    static SolicitudInfo desde(Solicitud s, Map<String, String> nombres) {
        return new SolicitudInfo(s.getId(), s.getDireccion(),
                s.getReferencia(), s.ubicacion(), List.copyOf(s.getMateriales()),
                nombres.getOrDefault(s.getOrganizacionId(), s.getOrganizacionId()),
                s.getCiudadano().getNombre(), s.getContacto(), s.getNota(),
                s.getEstado(), s.getCreadaEn(), s.getFinalizadaEn(), s.getVersion());
    }

    public boolean contactoEsTelefono() {
        return contacto != null && contacto.matches("^[+0-9 ().-]{6,}$");
    }

    // el detalle inline (mapa + nota) solo existe si hay algo que mostrar
    public boolean tieneDetalle() {
        return ubicacion != null || (nota != null && !nota.isBlank());
    }

    public String creada() {
        return Fechas.corta(creadaEn);
    }

    public String finalizada() {
        return Fechas.corta(finalizadaEn);
    }
}
