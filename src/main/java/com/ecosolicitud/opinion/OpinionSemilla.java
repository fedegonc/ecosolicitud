package com.ecosolicitud.opinion;

import java.time.Instant;

import com.ecosolicitud.shared.Rol;

// Una opinión precargada del dataset de demostración.
public record OpinionSemilla(int valor, String comentario, Rol rol, Instant creadaEn) {
}
