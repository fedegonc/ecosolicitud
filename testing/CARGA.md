# Pruebas de carga y capacidad

## 1. Qué significa capacidad

No hay un número universal de usuarios o solicitudes que rompa el plan gratuito de Render. Depende del patrón de navegación, tasa de peticiones, costo de cada endpoint, escrituras, tamaño de la base, CPU y memoria. Una solicitud de retiro es un registro del negocio; una petición HTTP es una operación sobre el servidor. No son la misma medida.

Distinguir:

- Personas con la página abierta: no generan carga HTTP constante.
- Usuarios activos: alternan peticiones y pausas.
- Peticiones simultáneas o por segundo: carga efectiva que recibe la app.
- Solicitudes de retiro acumuladas: volumen del dataset, no concurrencia.

Ejemplo orientativo de demanda: 50 personas que generan una petición cada 10 segundos producen aproximadamente 5 peticiones/s. Diez que generan una por segundo producen aproximadamente 10 peticiones/s. No incluye recursos estáticos, htmx ni pausas por respuestas lentas.

**Decisión actualizada:** la primera prueba con 10 usuarios fue correcta, pero la repetición sobre `22dc7fc` activó el corte por latencia. Diez usuarios intensivos no constituyen una garantía estable para el piloto. Los niveles de 15 y 20 quedan bloqueados hasta reproducir una referencia aceptable y explicar la variabilidad. No hay evidencia para prometer 25 o 50 usuarios activos ni un número de POST/s.

## 2. Plan disponible y límites

Plan: [carga-10-usuarios.jmx](../src/test/resources/carga-10-usuarios.jmx). Herramienta utilizada: Apache JMeter 5.6.3, sin plugins externos.

Defaults: 10 usuarios, rampa de 5 segundos, cinco recorridos, pausa de 500 ms antes de cada petición, keep-alive y cookies independientes por hilo. Cada recorrido realiza ocho GET:

1. `/`
2. `/acopios`
3. `/nueva?ciudad=RIVERA&organizacionId=frontera-limpia`
4. `/mis-solicitudes`
5. `/guia`
6. `/comunidad`
7. `/estadisticas`
8. `/opinion`

Cada muestra exige HTTP 200 y presencia de `<main`. No sigue redirecciones: una redirección inesperada no se oculta como éxito.

No descarga CSS/imágenes, no ejecuta JavaScript, no realiza POST y no prueba roles de organización. Excluye `/avisos`, porque ese GET marca avisos como leídos, y todos los endpoints de reinicio. Los cookies son independientes, pero la identidad `ciudadano-demo` sigue siendo compartida.

Las assertions no garantizan que cada página contenga todos los datos correctos: eso se verifica con las pruebas funcionales. Los resultados no representan tiempo de pintura ni experiencia visual en navegador.

## 3. Resultados ejecutados el 2026-10-04

### JMeter

| Métrica | Local, perfil dev | Render por HTTPS |
|---|---:|---:|
| Usuarios | 10 | 10 |
| Recorridos por usuario | 5 | 30 |
| Peticiones | 400 | 2.400 |
| Fallos HTTP/assertions | 0 | 0 |
| Promedio | 440,29 ms | 657,03 ms |
| Mediana | 399,5 ms | 583 ms |
| p95 | 891,9 ms | 1.298 ms |
| p99 | 1.153,58 ms | 1.988,97 ms |
| Máximo | 1.374 ms | 3.609 ms |
| Throughput del reporte | 9,73 peticiones/s | 8,50 peticiones/s |

La prueba remota duró aproximadamente 4 min 43 s. Fue ejecutada contra `https://ecosolicitud.onrender.com` entre 16:45:35 y 16:50:18 de Uruguay, equivalentes a 19:45:35–19:50:18 UTC. Se comprobó que la portada respondía antes y después.

La prueba local usaba cambios sin commit en `developer`; Render despliega desde `main` y no se registró el SHA activo de ese ensayo. No se puede atribuir la diferencia de tiempos exclusivamente al alojamiento: también cambian perfil, red, versión y condiciones de ejecución.

### Recursos consultados por MCP de Render

Ventana consultada: 19:40–19:55 UTC; resolución de 30 segundos; CPU consultada con agregación MAX. Servicio Free, Docker, Oregon, una instancia.

| Recurso | Observado |
|---|---|
| CPU asignada | 0,15 CPU |
| CPU máxima | 0,15 CPU, igual al 100 % de la cuota |
| Memoria límite | 512 MiB |
| Memoria antes de la prueba | 342,13 MiB |
| Memoria al finalizar | 388,45 MiB, aproximadamente 75,87 % del límite |
| Aumento de memoria | 46,32 MiB |
| Transferencia del intervalo horario 19:00 UTC | 40,07 MB, unidad informada por Render |

La CPU llegó al límite en varios intervalos; no es una medición del promedio de toda la prueba ni demuestra por sí sola throttling. No permite calcular el máximo de usuarios mediante una regla de tres.

El crecimiento de memoria no prueba una fuga: la JVM puede conservar memoria reservada. Debe compararse la tendencia en una prueba sostenida y su recuperación. La transferencia es volumen acumulado por hora, no MB/s ni uso de CPU.

Las consultas de métricas HTTP y logs de aplicación no devolvieron datos para esa ventana. El resultado HTTP proviene de JMeter. No interpretar ausencia de logs como garantía de ausencia de problemas.

### Evidencia

Resumen público: [resultados/2026-10-04.json](resultados/2026-10-04.json).

Artefactos locales originales, no publicados y eliminables por `mvn clean`:

- `target/carga-10-usuarios.jtl`
- `target/reporte-carga-10-usuarios/statistics.json`
- `target/render-20261004-164533.jtl`
- `target/reporte-render-20261004-164533/statistics.json`

**Conclusión de la primera ejecución:** diez usuarios de navegación completaron el escenario sin errores y con p95 de 1,30 s en Render. CPU fue el recurso con menor margen observado. No se midieron escrituras ni la capacidad máxima. La repetición siguiente impide tratar este dato como capacidad estable.

### Repetición controlada sobre `22dc7fc`: corte en el primer nivel

El 2026-10-04 se confirmó por MCP que `22dc7fcb17064b19cf0029c8b6a2b4b42a231e5b` estaba live. Se inició la campaña condicional 10 → 15 → 20 con el mismo JMX, 30 recorridos planificados, pausa de 500 ms y rampa de 15 segundos. No se modificaron datos ni configuración de Render.

Un supervisor local (`scratch/carga_controlada.py`, ayuda operativa no versionada) revisó el JTL cada dos segundos y ejecutó `shutdown.sh 4447` cuando el p95 de una ventana móvil de un minuto permaneció por encima de 3 s durante un minuto. También tenía cortes por 5xx, timeouts repetidos, tasa de errores y duración máxima operativa de 600 s. CPU y memoria se supervisaron por MCP, no automáticamente desde ese script. La campaña no avanzó al siguiente nivel.

| Métrica | Nivel de 10 usuarios |
|---|---:|
| Peticiones previstas | 2.400 |
| Peticiones completadas antes del corte | 226 |
| HTTP 200 / errores de assertions | 226 / 0 |
| Promedio | 2.264,83 ms |
| Mediana | 2.190 ms |
| p95 del reporte JMeter | 3.911,25 ms |
| p99 | 5.775,31 ms |
| Máximo | 6.976 ms |
| Throughput observado | 3,30 peticiones/s |
| Objetivo de navegación | No cumplido |
| Niveles 15 y 20 | No ejecutados: bloqueados por el criterio previo |

Tráfico aproximado entre 23:12:17 y 23:13:29 UTC; la preparación del generador y generación de informe se registran aparte. La ejecución quedó truncada: no comparar sus 226 muestras como si fueran otro recorrido completo de 2.400. El supervisor usó p95 por rango más cercano; JMeter informa percentiles interpolados, de modo que pueden diferir levemente.

Métricas de Render en 23:11–23:16 UTC: CPU máxima de 0,11833 sobre cuota 0,15, aproximadamente 79 %; memoria máxima de aproximadamente 354,66 MiB sobre 512 MiB, aproximadamente 69 %. En 23:15 UTC la CPU volvió a 0,00452. La consulta de logs de nivel error no devolvió entradas. Son datos agregados con posible retardo de muestreo, no medición por petición.

Después de detener JMeter la portada siguió devolviendo HTTP 200, con dos comprobaciones de 0,54 y 0,69 s. El servidor no cayó en las comprobaciones realizadas. El corte fue por latencia, no por falta de memoria ni un 500.

Evidencia agregada y series: [resultados/2026-10-04-escalones.json](resultados/2026-10-04-escalones.json). Informe local: `target/render-escalon-10-20261004-231143Z-reporte/`.

**Conclusión limitada:** esta repetición incumplió el criterio de tiempos, sin fallos funcionales observados. CPU no llegó a su cuota en la ventana consultada, por lo que no se demuestra que sea la única causa. El cambio de versión, calentamiento JVM, red, condiciones del alojamiento y del generador no están aislados. Próximo paso propuesto: comprobar por endpoint con pocos usuarios, separar calentamiento y repetir 10 con condiciones controladas; no forzar 15/20 antes de ese diagnóstico.

## 4. Ejecución reproducible

Instalar JMeter 5.6.3 desde su [sitio oficial](https://jmeter.apache.org/download_jmeter.cgi) y verificar la descarga. Java 21 sirve para ejecutar la versión utilizada. No forma parte de las dependencias ni del jar de la app.

Desde la raíz, con JMeter disponible en PATH:

```bash
mkdir -p target
RUN=$(date +%Y%m%d-%H%M%S)
JMETER_BIN=${JMETER_BIN:-jmeter}
HEAP='-Xms128m -Xmx512m -XX:MaxMetaspaceSize=256m' \
"$JMETER_BIN" -n -t src/test/resources/carga-10-usuarios.jmx \
  -Jhost=localhost -Jprotocolo=http -Jpuerto=8080 \
  -Jusuarios=10 -Jrampa=5 -Jiteraciones=5 -Jpausa=500 \
  -l "target/carga-$RUN.jtl" -j "target/carga-$RUN.log" \
  -e -o "target/reporte-carga-$RUN"
```

En la máquina donde se hicieron los ensayos se puede usar `export JMETER_BIN=target/apache-jmeter-5.6.3/bin/jmeter`, mientras no se elimine `target/`.

Para repetir el ensayo remoto autorizado: cambiar a `-Jhost=ecosolicitud.onrender.com -Jprotocolo=https -Jpuerto=443 -Jiteraciones=30`. Confirmar que el servicio responde antes, y medir arranque en frío por separado. No desactivar verificación TLS para resolver fallos de conexión.

Cada ejecución necesita JTL y directorio de reporte nuevos. JMeter puede terminar con exit code 0 aunque haya assertions fallidas: revisar `success`, errores y cantidad real de muestras. Fórmula del plan: usuarios × recorridos × 8. Es un modelo cerrado: si el servidor se vuelve lento, los usuarios esperan y baja la tasa; el throughput observado no es el máximo de capacidad.

La duración depende de las respuestas; los recorridos no equivalen a minutos. No llamar al plan de cinco recorridos una prueba de resistencia prolongada.

## 5. Objetivos propuestos y criterios de parada

Estos son objetivos del piloto, no garantías de Render ni límites ya medidos:

- Navegación: p95 <= 2 s y cero fallos inesperados.
- Datos: ninguna operación perdida, duplicada involuntariamente o aplicada a otra identidad.
- Recuperación: tras retirar la carga, los tiempos y CPU vuelven a una línea base comparable.

Detener la ejecución y no subir al escalón siguiente si aparece:

1. Reinicio, falta de memoria o cualquier 500 inesperado confirmado.
2. Timeouts repetidos o más de 1 % de muestras fallidas en una ventana de un minuto.
3. p95 > 3 s sostenido durante un minuto, aunque todavía haya respuestas 200.
4. Memoria >= 90 % de la cuota, aproximadamente 461 MiB, durante un minuto.
5. CPU cerca de su cuota junto con empeoramiento sostenido de tiempos. Un máximo aislado de CPU no basta para declarar fallo.

El JMX actual no implementa estos cortes automáticamente. Requiere un operador mirando resultados y métricas. Para detenerlo de forma ordenada, ejecutar `shutdown.sh` o `shutdown.cmd` de la instalación JMeter que lanzó el ensayo. En este repositorio hay una instalación local bajo `target/apache-jmeter-5.6.3/bin/`. No lanzar varias instancias para superar artificialmente los límites.

## 6. Campaña y estado de ejecución

| ID | Escenario | Entorno y condición | Estado |
|---|---|---|---|
| C-01 | 1 usuario × 1 recorrido para validar rutas y assertions | Local o remoto autorizado, sin POST | Ejecutado local, 8 muestras correctas |
| C-02 | 10 usuarios de navegación | Local dev y Render | Ejecutado; resultados arriba |
| C-03 | 5 → 10 → 15 → 20 usuarios, rampa de 15 s y 30 recorridos por nivel | Ejecutar un nivel por vez, aplicar cortes y esperar recuperación | Repetición desde 10 detenida por latencia; 15/20 bloqueados. El nivel 5 sigue pendiente |
| C-04 | 10 usuarios con 100 recorridos y pausa de 500 ms | Resistencia: registrar duración real, tendencia de RAM y recuperación | Pendiente |
| C-05 | 10 usuarios, un recorrido, rampa de 1 s y sin pausa | Ráfaga acotada de 80 GET, primero en entorno aislado | Pendiente |
| C-06 | Navegación y creación de solicitudes con identidades distintas | Base descartable aislada, CSRF real y datos sintéticos | Pendiente; requiere plan de POST distinto |
| C-07 | Dos actores de la misma organización aceptan la misma solicitud y versión | Dos transacciones realmente solapadas; exactamente un cambio efectivo y un aviso | Pendiente; no basta enviar una versión vieja secuencialmente |
| C-08 | Dos ediciones simultáneas del perfil | Mismo criterio: una gana, otra informa conflicto, ninguna sobrescritura silenciosa | Pendiente |
| C-09 | Dataset de 5, 100, 500 y 1.000 solicitudes ficticias | Generar en base aislada, nunca reemplazar datos del servicio compartido | Pendiente; medir listados y estadísticas con mismo patrón |
| C-10 | Inicio en frío y primer GET después de inactividad natural | Registrar por separado, sin suspender ni reiniciar el servicio como parte del ensayo | Pendiente |

Un nivel solo pasa si cumple tiempos, errores, datos y recuperación. La primera infracción sostenida identifica un límite operativo del escenario, no una cantidad universal que rompe el sistema. Un lote total de 2.400 GET ya fue exitoso; eso no implica que 2.400 GET simultáneos o 2.400 retiros creados sean seguros.

## 7. Problemas anticipados y soluciones proporcionadas

| Riesgo | Evidencia/estado | Acción antes de optimizar |
|---|---|---|
| CPU limitada | Cuota alcanzada con 10 usuarios en varios intervalos | Verificar perfil prod y versión; medir endpoint/consulta costosa. Si persiste, escalonar participantes o evaluar más CPU con aprobación |
| Identidad demo compartida | Todos los ciudadanos usan `ciudadano-demo` | Asignar identidades ficticias distintas para piloto autónomo; no confundir CookieManager con aislamiento de datos |
| Reinicio global | `/demo/reiniciar` reemplaza el dataset para todos | Reservar operación al facilitador y retirar acceso público antes del piloto compartido |
| Conflictos de actualización | Existe @Version, pero falta prueba simultánea | C-07/C-08; si se confirma 500 al commit, tratar conflicto fuera de la transacción fallida |
| Reenvío o doble clic | Crear no tiene garantía explícita de idempotencia | Probar duplicados; limitar doble envío en UI y usar protección del servidor si el negocio exige exactamente una solicitud |
| Memoria y consultas crecen con el historial | Hay listados y métricas basados en colecciones completas | C-09 antes de paginar o introducir agregados; 50 personas no acotan el historial de meses |
| Pool de conexiones | Sin saturación del pool demostrada | Medir espera y duración transaccional; no aumentar conexiones o threads a ciegas |
| Datos H2 en almacenamiento efímero | `update` no garantiza volumen persistente | Aceptable solo para datos descartables; verificar persistencia o backup antes de retiros reales |
| Arranque en frío | No medido en esta campaña | C-10, medir y comunicar por separado |

No cambiar de base, agregar caché global, introducir microservicios ni subir de plan como solución automática. Las solicitudes y los avisos son datos cambiantes: una caché indiscriminada puede mostrar información incorrecta.
