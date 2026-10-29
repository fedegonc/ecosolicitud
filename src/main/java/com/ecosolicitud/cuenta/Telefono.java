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

    // cómo se muestra en campos de la interfaz (inverso de normalizar):
    // "59899111222" → "+598 99 111 222", "5551991234567" → "+55 51 99123-4567"
    public static String presentar(String normalizado) {
        if (normalizado == null) {
            return null;
        }
        if (CELULAR_UY.matcher(normalizado).matches()) {
            var n = normalizado.substring(3);
            return "+598 " + n.substring(0, 2) + " " + n.substring(2, 5)
                    + " " + n.substring(5);
        }
        if (CELULAR_BR.matcher(normalizado).matches()) {
            var n = normalizado.substring(2);
            return "+55 " + n.substring(0, 2) + " " + n.substring(2, 7)
                    + "-" + n.substring(7);
        }
        return "+" + normalizado;
    }

    private static int largoCompleto(Pais pais) {
        return pais == Pais.UY ? 11 : 13;
    }

    // el 0 de "099 123 456" o "055 99123-4567" es prefijo de discado local
    private static String sinCeroInicial(String digitos) {
        return digitos.startsWith("0") ? digitos.substring(1) : digitos;
    }
}
