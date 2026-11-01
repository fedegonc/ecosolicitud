# Resultados y hallazgos consolidados

Síntesis de lo medido con fecha, método y límite de cada dato. Las ejecuciones
completas están en `resultados/*.json`, `../scratch/resultados-carga/` y
`../docs/ux/mediciones-*.json`. Estados según README: ejecutado / pendiente /
hipótesis / bloqueado.

## 1. Corrección funcional — ejecutado

- Última verificación conocida: `mvn -B -DfailIfNoTests=true verify` con
  **79 ejecuciones JUnit, 0 fallos, 0 errores** + gate de arquitectura
  (`ModulesTest`). Desglose honesto en [REGLAS.md](REGLAS.md): 19 métodos
  normales + parametrizados; el conteo no es la meta.
- Trazabilidad: E-01..E-08 ejecutados; E-09, E-10, E-11, E-12 pendientes con
  próximo paso declarado en [E2E.md](E2E.md). R-01..R-10 pasados.
- Límite: integración HTTP con HtmlUnit; no demuestra renderizado real ni
  flujo htmx completo (eso se cubre en la evaluación de interfaz, sección 4).

## 2. Integridad bajo concurrencia — ejecutado, bug real corregido

Escenario: 25 sesiones aceptan **la misma solicitud con la misma versión**
(`carga-escrituras.jmx`, SynchronizingTimer). Esperado: 1 ganador,
N-1 conflictos, 0 errores (`resultados/2026-10-06-escrituras.json`).

| Ejecución | Commit | Resultado |
|---|---|---|
| Pre-fix (Render) | `643e2a9` | 1 ganador, 18 conflictos, **6 HTTP 500** |
| Post-fix local | `ed191d4` | 1 ganador, 24 conflictos, **0 errores** |
| Post-fix producción | `ed191d4` | igual: 1/24/0, 105 muestras, 0 % error |

- Causa raíz: `catch` de `OptimisticLockingFailureException` dentro de
  `@Transactional` dejaba la TX rollback-only y el commit lanzaba
  `UnexpectedRollbackException` al cliente.
- Regresión permanente: `SolicitudConcurrenciaTest` reproduce el fallo sin
  hilos (cache de persistencia + REQUIRES_NEW).
- Límite: latencia del intento en producción media 11,2 s (mín 1,7, máx 18,3):
  la espera del timer domina; no es latencia de la operación aislada.

## 3. Capacidad del despliegue — ejecutado (Render Free: 0,15 CPU / 512 MB / 1 instancia)

Campaña de escalones de navegación GET, sin escrituras
(`resultados/2026-10-05-escalones.json`, commit `b616668`):

| Usuarios | Muestras | Errores | Mediana | p95 | p99 | Máx | Req/s |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 5 | 400 | 0 | 297 ms | 501 ms | 594 ms | 717 ms | 5,26 |
| 10 | 2400 | 0 | 456 ms | 811 ms | 1084 ms | 1898 ms | 9,66 |
| 15 | 2400 | 0 | 612 ms | 1305 ms | — | 2293 ms | 11,53 |
| 20 | 2400 | 0 | 802 ms | 1884 ms | — | 4499 ms | 12,78 |

Ráfaga agresiva: 30 usuarios, rampa 2 s, sin pausa
(`resultados/2026-10-05-rafaga.json`):

- 929/1200 muestras, 1 error (espera > timeout 20 s); parada por criterio
  "p95 > 3 s sostenido un minuto". Mediana 1766 ms, p95 4659 ms, máx 40 s.
- Memoria al 86 % del límite (462,5/512 MiB); **sin caída ni reinicio**;
  recuperación inmediata (HTTP 200 en 0,26-0,36 s al retirar la carga).
- **Techo observado del plan Free: ~13-14 req/s** de páginas HTML. Conversión
  a uso real: equivale a ~150-400 personas navegando a la vez; muy por encima
  del objetivo de 500 usuarios/día.

Ejecuciones posteriores (2026-10-27, `medicion-A/B/rendimiento` en
`scratch/resultados-carga/`): las corridas se detuvieron por el mismo criterio
de p95 (medianas de 3-7 s). Métricas de plataforma descargadas con
`recursos_render.py` para esas ventanas: **CPU al 100 % de la cuota de
0,15 vCPU de forma sostenida durante las tres corridas** (memoria ~75 % del
límite), mientras que en la campaña del 05-10 el nivel de 20 usuarios no
saturó así. Causa probable: saturación de CPU por cuota del plan; pendiente
determinar si el cambio frente al 05-10 vino del commit, del tamaño del
dataset o de varianza de la plataforma compartida. **No se usa como línea de
base.** Reportes por corrida: `recursos.html` en cada directorio de campaña.

Lección operativa: un `mvn clean` durante la generación del reporte borró un
JTL en `target/`; las salidas de campaña ahora van fuera de `target/`.

## 4. Usabilidad e interfaz — ejecutado (evaluación de experto, no prueba con usuarios)

Evaluación heurística del 2026-10-15 (`../docs/ux/informe-heuristico.md`,
línea de base `mediciones-2026-10-15.json`, capturas en `docs/ux/capturas/`):

- **Tareas T1-T10: 8 completan, 1 parcial, 1 no.** T1 (día del camión) parcial:
  el atajo lleva a la lista de la guía, no a la respuesta. T6 (centro en el
  mapa) no se completa: el seed no carga ubicación ni dirección textual.
- **20 hallazgos** (escala Nielsen): 0 bloqueantes; 3 de gravedad 3 — F1
  mezcla de idiomas al navegar a Livramento con `hx-boost`; F2 el primer clic
  en "Enviar solicitud" se pierde si cae durante la validación en vuelo
  (verificado por red: `mousedown` sin `submit` ni POST); F3 atajo "Por día"
  no responde la pregunta.
- Métricas objetivas de la línea de base: 0 scroll horizontal en todas las
  páginas medidas (ES/PT, 1366 y 360 px); piso de 5 nodos interactivos <24 px
  por página; portada 269 KB y guía 327 KB en frío; 6 imágenes sin
  `loading=lazy`; 50 variables CSS `:root` (11 sin uso); paridad de claves
  i18n ES/PT 274/274.
- Plan de mejora: 30 ítems (B1-B30) priorizados en 3 entregas, cada uno con
  criterio de verificación medible (`../docs/ux/backlog.md`).
- Límites declarados: evaluación de experto ≠ prueba con usuarios reales;
  axe-core cubre ~30-40 % de los problemas de accesibilidad; localhost no
  refleja red real. La campaña manual en navegador M-01..M-10 sigue pendiente.

## 5. Disponibilidad observada — ejecutado (monitor externo)

UptimeRobot sobre `https://ecosolicitud.onrender.com` cada 5 min, alerta por
email (up/down). Ventana 2026-10-25 → 2026-11-01:

- **Uptime ~81 %**; 2 incidentes: 502 de 5 min (2026-10-26, compatible con un
  deploy) y **503 de 1 d 7 h 53 m** (2026-10-30 17:06 → 2026-11-01 ~01:00 UTC).
- Causa del 503: agotamiento de las horas del plan Free (pool compartido
  ~750 h/mes entre los servicios del workspace; el propio monitor mantiene el
  servicio despierto ~720 h/mes). Reinicio automático al renovarse la cuota
  el día 1. **Es una limitación del entorno, no un defecto del software** —
  declarado así en cualquier comparación de disponibilidad.
- Gaps de monitoreo detectados: `healthCheckPath` vacío en Render (la
  plataforma no distingue "vivo" de "roto"); el monitor cubre solo la portada
  (un 200 con error genérico cuenta como UP); recordatorio SSL del contacto
  desactivado.
- Próximo paso propuesto: Actuator `/actuator/health` + keyword monitor;
  maintenance window mensual solo si se quiere separar la cuota de los
  incidentes reales.

## 6. Experimento propuesto, no ejecutado

- **H2 embebido vs. Postgres (Supabase)** bajo el mismo plan JMeter:
  `application-prod.properties` ya acepta `DATABASE_URL` y el driver
  `postgresql` está en el `pom.xml`. Protocolo: calentamiento + 2 corridas
  por motor con `carga_controlada.py`; reportar mediana/p95/throughput.
  Variables a declarar: dataset idéntico, pool por defecto, latencia de red
  si se usa Supabase remoto vs. Postgres local.
- Hipótesis honesta: H2 puede ganar en latencia de operación simple (sin
  red ni IPC); Postgres se justifica por concurrencia, durabilidad y
  operación, no necesariamente por velocidad bruta a esta escala.

## Cómo reproducir

```bash
# funcional
mvn -B -DfailIfNoTests=true verify
# carga (con la app levantada o contra Render)
python3 scratch/carga_controlada.py <usuarios> --jmx src/test/resources/carga-10-usuarios.jmx --salida <dir>
python3 scratch/graficar_carga.py <dir>
# métricas de interfaz
cd docs/ux && node medir.js   # app en localhost:8080
```
