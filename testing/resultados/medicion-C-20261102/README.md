# Medición C — 10, 15 y 20 personas concurrentes

Fecha: 2026-11-02 (18:49–19:24 UTC). Sistema: https://ecosolicitud.onrender.com
(commit `a2b3a9b`, Render Free Oregon, ~0,15 vCPU, 512 MB). Recorrido de
navegación con sesión demo (`carga-10-usuarios-demo.jmx`), rampa 15 s, pausa
500 ms, 30 iteraciones por persona. Límite de ingreso relajado a 60/min vía
`ECOSOLICITUD_CUENTA_INTENTOS` solo durante la prueba — **volver a quitarla en
Render después para que rija el default de 20**.

## Resultados

| Personas | Pedidos | Errores | Mediana | p95 | Pedidos/s | Resultado |
|---|---|---|---|---|---|---|
| 10 | 2.440 | 0 | 1,00 s | 2,00 s | 6,06 | Completó |
| 15 | 3.660 | 0 | 1,60 s | 2,59 s | 6,82 | Completó |
| 20 | 483* | 0 | 2,30 s | 5,01 s | 6,16 | Cortado (umbral interno 3 s) |
| 20 (reintento, umbral 5 s) | 4.088 | 0 | 2,30 s | 3,96 s | 6,83 | Cortado por tope de 10 min |

\* La primera corrida con 20 se detuvo por la regla interna del script
(p95 > 3 s durante 1 min), más estricta que la del protocolo (5 s). Con el
umbral correcto completó 83 % de los pedidos en el tope operativo de 10 min,
sin errores. El límite del plan aparece en latencia, no en fallas.

CPU y memoria del servicio en `datos/recursos-*.json` (panel de Render: CPU al
100 % del cupo durante los tres niveles; memoria estable ~55 %).

## Error controlado

`GET /no-existe-controlado` → 404 esperado: queda en el log de pedidos de
Render y en el contador `pedidos_4xx` de `/metricas` (endpoint nuevo en esta
rama). Un 5xx real queda cubierto por `TiemposFiltro` (WARN en log +
`pedidos_5xx`); `MetricasTest` lo verifica automáticamente.

## Conclusión

El plan gratuito aguanta 10–15 personas concurrentes sin errores y con tiempos
aceptables; con 20 personas sostiene ~7 pedidos/s sin fallas pero la espera
sube (~2,3 s mediana, ~4 s p95). Es decir: degrada en velocidad, no en
estabilidad. Para la prueba de 20 personas de campo el sistema va justo pero
viable; el cuello es CPU (siempre al 100 %), no memoria ni errores.

## Repetir

```bash
python3 scratch/carga_controlada.py 10 --iteraciones 30 \
    --jmx scratch/carga-10-usuarios-demo.jmx --salida scratch/resultados-carga/medicion-C-<fecha>
# ídem con 15 y 20 (20 con --umbral-ms 5000); ~2 min de espera entre niveles
python3 scratch/recursos_render.py <dir> --desde <inicio-UTC>
python3 scratch/graficar_medicion_simple.py <dir> --salida testing/resultados/medicion-C-<fecha>
```
