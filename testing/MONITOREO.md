# Monitoreo mínimo

Qué se mira y dónde, en lenguaje simple.

## Qué hay

| Dónde | Qué muestra | Cuándo usarlo |
|---|---|---|
| `GET /salud` | `{"estado":"ok"}` — el servicio está vivo | Despertar el plan free (se duerme a los 15 min), checks externos |
| `GET /metricas` | Contadores agregados: `pedidos_2xx`, `pedidos_4xx`, `pedidos_5xx`, `p95_ms_ultimos` (últimos 512 pedidos), `memoria_usada_mb`, `segundos_arriba` | Durante/tras la carga, ver si hay errores y latencia |
| Log de Render | `WARN metodo /ruta estado ms` por pedido lento (≥1 s) o 5xx (filtro `ecosolicitud.tiempos`) | Diagnosticar qué rutas fallan |
| Panel de Render | CPU y memoria del servicio, request count, deploys | Saturación del plan y restarts |

## Límites

- `/salud` es público y no devuelve nada sensible; `/metricas` pide sesión.
- Los contadores viven en memoria: se reinician en cada deploy/restart.
- Los tiempos no llevan IP, query ni usuario; los ids salen como `{id}`.

## Interpretación rápida

- `pedidos_5xx` > 0 → buscar el `WARN` correspondiente en el log.
- `p95_ms_ultimos` sostenido > 5000 → criterio de corte de la campaña.
- `memoria_usada_mb` cerca del límite del plan (512 MB) → riesgo de reinicio.
