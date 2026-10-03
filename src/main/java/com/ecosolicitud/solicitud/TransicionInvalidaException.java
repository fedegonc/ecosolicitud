package com.ecosolicitud.solicitud;

public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(Estado estado, String accion) {
        super(estado + " no admite " + accion);
    }
}
