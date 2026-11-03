# EcoSolicitud

App para pedir retiros de reciclables en Rivera y Sant'Ana do Livramento.
Spring Boot + Thymeleaf + htmx, español/portugués, PWA.

- Desarrollo local: `mvn spring-boot:run`
- Pruebas y revisiones: `mvn verify`
- Medición de carga y monitoreo: `testing/CARGA.md`, `testing/MONITOREO.md`
- Producción: https://ecosolicitud.onrender.com (Render Free, Docker)

Detalle técnico en `testing/` y `docs/`.

## Medir una prueba con participantes

Antes y después de la sesión, guardar hora, commit desplegado, cantidad de
participantes y una captura de `/metricas` con una cuenta autenticada. Comparar
los contadores antes/después; se reinician al reiniciar el servidor.

Durante la prueba:

- Revisar errores y pedidos lentos en los logs de Render.
- Mirar CPU, RAM total y reinicios en Render; conexiones y consultas en Supabase.
- Registrar por tarea si se completó, cuánto demoró y qué problema encontró la persona.
- Confirmar los registros esperados en la base y exportar las respuestas al terminar.
- No ejecutar carga artificial ni desplegar mientras participan personas.

Límites: `p95_ms_ultimos` mezcla las últimas 512 peticiones, no mide cada tarea
ni la pintura del navegador. `memoria_usada_mb` mide heap Java usado, no RAM
total del proceso. Actualmente `pedidos_2xx` también incluye redirecciones.
`/salud` comprueba que la app responde, no la conexión con la base. Las métricas
no reemplazan una prueba del flujo completo ni un respaldo de datos.
