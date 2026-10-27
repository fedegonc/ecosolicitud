package com.ecosolicitud.shared;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

// Guarda cada valor durante un tiempo fijo y lo recalcula al vencer. Pensada
// para pocas claves (una por período de estadísticas). Si dos pedidos llegan
// juntos con la entrada vencida, los dos pueden calcular: es aceptable y evita
// bloquear mientras se consulta la base.
public final class CacheConVencimiento<K, V> {

    private record Entrada<V>(V valor, Instant calculadaEn) {
    }

    private final Duration vida;
    private final Clock reloj;
    private final Map<K, Entrada<V>> entradas = new ConcurrentHashMap<>();

    public CacheConVencimiento(Duration vida, Clock reloj) {
        this.vida = vida;
        this.reloj = reloj;
    }

    public V obtener(K clave, Supplier<V> calculo) {
        var ahora = reloj.instant();
        var entrada = entradas.get(clave);
        if (entrada != null && ahora.isBefore(entrada.calculadaEn().plus(vida))) {
            return entrada.valor();
        }
        var valor = calculo.get();
        entradas.put(clave, new Entrada<>(valor, ahora));
        return valor;
    }

    public void vaciar() {
        entradas.clear();
    }
}
