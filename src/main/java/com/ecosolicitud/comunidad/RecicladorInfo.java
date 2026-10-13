package com.ecosolicitud.comunidad;

import com.ecosolicitud.shared.Ciudad;

// Una persona que recicla informalmente por su cuenta, sin vínculo con centros.
// El padrón la documenta; no participa en solicitudes.
public record RecicladorInfo(String nombre, Ciudad ciudad, String telefono) {
}
