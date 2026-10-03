package com.ecosolicitud.solicitud;

public class TransicionInvalidaException extends RuntimeException {

    private final Estado estado;
    private final String accion;

    public TransicionInvalidaException(Estado estado, String accion) {
        super(estado + " no admite " + accion);
        this.estado = estado;
        this.accion = accion;
    }

    public Estado getEstado() {
        return estado;
    }

    public String getAccion() {
        return accion;
    }
}
