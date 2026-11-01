# Medición B — repetición de medicion-A sobre el sistema publicado

Repite el escenario de `medicion-A-20261027` contra `https://ecosolicitud.onrender.com`.

## Condiciones

| Condición | Valor |
|---|---|
| Fecha/hora | 2026-11-01, corridas 23:01:00 → 23:13:52 UTC (19:01–19:14 UYT) |
| Commit | `92c0308` (con cambios sin commit en `SeguridadConfig`, `Volver`, `SolicitudE4Test`) |
| Plan Render | Free: 0,15 vCPU, 512 MB, 1 instancia, región Oregon |
| Base de datos | H2 en archivo, filesystem del mismo servicio |
| Herramienta | JMeter 5.6.3 vía `scratch/carga_controlada.py` |
| Plan | `carga-10-usuarios-demo.jmx` (copia en `datos/`): las mismas 8 páginas GET de siempre — `/`, `/acopios`, `/nueva?ciudad=RIVERA&organizacionId=frontera-limpia`, `/mis-solicitudes`, `/guia`, `/comunidad`, `/estadisticas`, `/opinion` — más un ingreso **una vez por hilo**: login de la cuenta de centro semilla → `POST /demo/rol` (ciudadano demo) |
| Fases | Calentamiento: 5 personas × 3 recorridos. Tandas 1 y 2: 10 personas × 30 recorridos cada una, espalda con espalda. Rampa 15 s, pausa 500 ms entre páginas — igual que en A |
| Alcance | Solo lectura: ningún registro ni solicitud creada; los únicos POST son los dos del ingreso demo por hilo |
| Estado previo | Servicio verificado en 200 antes de medir (despierto por el monitor externo); sin arranque en frío en la medición |

Nota de cobertura: los gráficos incluyen en las primeras ventanas de cada fase
las solicitudes de ingreso demo (4 por hilo: página de ingreso, login, bandeja
y cambio de rol). El resumen por fase usa los datos completos del `.jtl`.

## Los 4 gráficos

| PNG | Pregunta | Conclusión en una línea |
|---|---|---|
| `1-carga.png` | ¿Cuánta carga recibió el sistema? | Pico de 8 pedidos por segundo con 10 personas a la vez |
| `2-espera.png` | ¿Cuánto esperó cada persona? | Sin errores: la mitad esperó 0,8 s o menos; el 95 %, 1,4 s o menos — bajo la meta de 2 s |
| `3-recursos.png` | ¿Qué recurso se agotó? | El procesador tocó el 100 % del límite durante las tandas de 10; la memoria se mantuvo ~54 % |
| `4-resumen.png` | ¿Cómo quedó cada fase? | Tabla por fase (también en `resumen.csv`) |

## Resultado por fase

| Fase | Personas | Pedidos/s | Espera típica (s) | Espera 95 % (s) | Errores | CPU máx | Mem máx |
|---|---:|---:|---:|---:|---:|---:|---:|
| Calentamiento | 5 | 3,2 | 0,53 | 1,30 | 0 | 41 % | 52 % |
| Tanda 1 | 10 | 6,8 | 0,89 | 1,43 | 0 | 100 % | 53 % |
| Tanda 2 | 10 | 7,0 | 0,81 | 1,34 | 0 | 100 % | 54 % |

## Comparación con medicion-A (2026-10-27)

| | A (27-10) | B (01-11) |
|---|---|---|
| Completitud | Las 3 corridas detenidas por p95 > 3 s sostenido | Las 3 fases completas (4920/4920) |
| Mediana | 2,97–3,52 s | 0,81–0,89 s |
| p95 | 4,7–6,2 s | 1,34–1,43 s |
| Errores | 0 | 0 |
| CPU | ~100 % **sostenido, ya antes de la carga** | ~5 % antes; 100 % solo durante las tandas |

**Qué cambió.** El dato nuevo de `recursos-*.json` lo aclara: en A la CPU ya
estaba al 85-100 % *antes* de que empezara JMeter — la instancia llegó
saturada a la medición (carga previa o vecino ruidoso en la plataforma
compartida). En B la CPU partía de ~5 % y subió solo con la prueba.

**Conclusión revisada.** La degradación de A no fue "10 personas saturan el
sistema": fue una instancia ya estrangulada antes de medir. Con CPU
disponible, el mismo escenario sostiene ~7 pedidos/s con p95 < 1,5 s, y
aunque el procesador vuelve a tocar el 100 % en las tandas, la latencia se
mantiene bajo criterio. El recurso limitante del plan Free sigue siendo la
CPU de 0,15 núcleos.

**Diferencia de escenario (honesta).** A navegó anónima; desde el 27-10
`/nueva`, `/mis-solicitudes` y `/opinion` pasaron a exigir sesión
(302 → `/cuenta/entrar`, cambio intencional). B entró una vez por hilo como
ciudadano demo. La corrida anónima de control quedó en
`scratch/resultados-carga/medicion-B-20261101-anonima/` (37,5 % de 302: las
3 páginas protegidas).

## Datos

`datos/` contiene los `.jtl`, los `.resumen.json`, el `recursos-*.json` de la
ventana y el plan usado. Reproducción: `python3 scratch/carga_controlada.py
<N> --jmx scratch/carga-10-usuarios-demo.jmx --salida <dir>` (el plan se
regenera con `scratch/jmx_demo.py`).
