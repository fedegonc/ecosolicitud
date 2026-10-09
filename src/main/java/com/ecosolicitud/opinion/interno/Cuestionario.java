package com.ecosolicitud.opinion.interno;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;

// Copia local de un formulario de Google: se muestra solo si Google falla.
public record Cuestionario(String titulo, List<Pregunta> preguntas) {

    static final int MAX_TEXTO = 1000;

    public enum Tipo { ESCALA, TEXTO, OPCION }

    public record Pregunta(Tipo tipo, String texto, List<String> opciones) {
    }

    public record Respuesta(String pregunta, String valor) {
    }

    static Cuestionario leer(List<String> lineas) {
        String titulo = null;
        var preguntas = new ArrayList<Pregunta>();
        for (String linea : lineas) {
            linea = linea.strip();
            if (linea.isEmpty() || linea.startsWith("#")) {
                continue;
            }
            int dosPuntos = linea.indexOf(':');
            if (dosPuntos < 0) {
                throw new IllegalArgumentException("Línea sin tipo: " + linea);
            }
            String clave = linea.substring(0, dosPuntos).strip();
            String resto = linea.substring(dosPuntos + 1).strip();
            switch (clave) {
                case "titulo" -> titulo = resto;
                case "escala" -> preguntas.add(new Pregunta(Tipo.ESCALA, resto, List.of()));
                case "texto" -> preguntas.add(new Pregunta(Tipo.TEXTO, resto, List.of()));
                case "opcion" -> {
                    var partes = Arrays.stream(resto.split("\\|")).map(String::strip).toList();
                    if (partes.size() < 3) {
                        throw new IllegalArgumentException("Opción con menos de dos alternativas: " + linea);
                    }
                    preguntas.add(new Pregunta(Tipo.OPCION, partes.get(0), partes.subList(1, partes.size())));
                }
                default -> throw new IllegalArgumentException("Tipo desconocido: " + clave);
            }
        }
        if (titulo == null || preguntas.isEmpty()) {
            throw new IllegalArgumentException("Cuestionario sin título o sin preguntas");
        }
        return new Cuestionario(titulo, List.copyOf(preguntas));
    }

    // Todas las preguntas son opcionales, como en Google; vacío si algún valor
    // es inválido o si no se respondió ninguna escala.
    Optional<List<Respuesta>> responder(IntFunction<String> valorDe) {
        var respuestas = new ArrayList<Respuesta>();
        boolean algunaEscala = false;
        for (int i = 0; i < preguntas.size(); i++) {
            var p = preguntas.get(i);
            String valor = valorDe.apply(i);
            valor = valor == null ? "" : valor.strip();
            if (valor.isEmpty()) {
                respuestas.add(new Respuesta(p.texto(), "—"));
                continue;
            }
            boolean valido = switch (p.tipo()) {
                case ESCALA -> valor.matches("[1-5]");
                case OPCION -> p.opciones().contains(valor);
                case TEXTO -> valor.length() <= MAX_TEXTO;
            };
            if (!valido) {
                return Optional.empty();
            }
            algunaEscala |= p.tipo() == Tipo.ESCALA;
            respuestas.add(new Respuesta(p.texto(), valor));
        }
        return algunaEscala ? Optional.of(List.copyOf(respuestas)) : Optional.empty();
    }
}
