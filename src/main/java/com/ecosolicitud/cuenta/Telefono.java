package com.ecosolicitud.cuenta;

import java.util.regex.Pattern;

// Un celular se guarda siempre igual: solo dígitos y con código de país, así
// "099 123 456" y "+598 99 123 456" son la misma cuenta. El país del selector
// solo decide cuando el número no trae prefijo internacional (+ o 00): el
// código de área 55 de Livramento choca con el código de país de Brasil.
public final class Telefono {

    private static final Pattern FORMATO = Pattern.compile("\\+?[0-9 ().\\-]+");
    private static final Pattern CELULAR_UY = Pattern.compile("5989\\d{7}");
    private static final Pattern CELULAR_BR = Pattern.compile("55[1-9]{2}9\\d{8}");

    private Telefono() {
    }

    public static String normalizar(String numero, Pais pais) {
        if (numero == null || pais == null) {
            throw new IllegalArgumentException("teléfono o país vacío");
        }
        var limpio = numero.strip();
        // letras u otros símbolos no se descartan en silencio: "juan 099123456" no es un número
        if (!FORMATO.matcher(limpio).matches()) {
            throw new IllegalArgumentException("caracteres inválidos en el teléfono: " + numero);
        }
        var digitos = limpio.replaceAll("\\D", "");
        String completo;
        if (limpio.startsWith("+")) {
            completo = digitos;
        } else if (digitos.startsWith("00")) {
            completo = digitos.substring(2);
        } else if (digitos.startsWith(pais.codigo()) && digitos.length() == largoCompleto(pais)) {
            completo = digitos;
        } else {
            completo = pais.codigo() + sinCeroInicial(digitos);
        }
        if (!CELULAR_UY.matcher(completo).matches() && !CELULAR_BR.matcher(completo).matches()) {
            throw new IllegalArgumentException("no es un celular de Uruguay ni de Brasil: " + numero);
        }
        return completo;
    }

    private static int largoCompleto(Pais pais) {
        return pais == Pais.UY ? 11 : 13;
    }

    // el 0 de "099 123 456" o "055 99123-4567" es prefijo de discado local
    private static String sinCeroInicial(String digitos) {
        return digitos.startsWith("0") ? digitos.substring(1) : digitos;
    }
}
