package com.ecosolicitud.shared;

// Largos de los campos que escribe el usuario: una sola fuente para la
// columna (@Column), la validación (@Size) y el maxlength de la plantilla
// (${limites.X}, expuesto por NavegacionAdvice).
public final class Limites {

    public static final int NOMBRE = 80;
    public static final int CONTACTO = 40;
    public static final int DIRECCION = 120;
    public static final int REFERENCIA = 120;
    public static final int NOTA = 300;
    public static final int HORARIO = 80;
    public static final int TELEFONO = 30;
    public static final int RESPUESTA_TEXTO = 1000;

    private Limites() {
    }
}
