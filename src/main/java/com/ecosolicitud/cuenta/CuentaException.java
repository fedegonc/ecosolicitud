package com.ecosolicitud.cuenta;

// campo: el input del formulario que se marca con el error;
// mensaje: la clave de messages que lo explica
public class CuentaException extends RuntimeException {

    private final String campo;

    public CuentaException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String campo() {
        return campo;
    }
}
