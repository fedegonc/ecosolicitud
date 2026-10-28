# Informe: ensayo del seminario (50 usuarios concurrentes)

## Resultados de carga
- 50 usuarios: **40 OK, 0 errores 5xx, 0 respuestas 429**
- Tiempo total del ejercicio: ~8 s · **p95 por usuario ≈ 3 s**
- Sin leaks de sesión medibles (+40 sesiones ≈ 0 MiB)

## Hallazgos con severidad

### 🔴 Alta — doble submit crea duplicados
POST `/nueva` repetido con el mismo CSRF crea 2 solicitudes. En la demo, un
doble clic de asistente crea duplicado visible. Sin guarda de idempotencia.
**Fix sugerido:** deduplicar por (ciudadanoId, organizacion, materiales,
dirección) dentro de una ventana corta, o token de envío único.

### 🟠 Media — registro bajo ráfaga (~10/50 fallan en el sim)
Los primeros ~10 registros concurrentes vuelven con el formulario re-render
(200 sin "Cuenta creada"). Patrón compatible con saturación del pool Hikari
(10 conexiones, 16 hilos) o validación. Diagnóstico exacto pendiente: capturar
el cuerpo de la respuesta de registro fallido.

### 🟢 OK — race aceptar×cancelar: consistente
Con `version` en los formularios, ambas acciones fueron rechazadas y la
solicitud quedó PENDIENTE — sin corrupción. El optimistic locking funciona.

### 🟡 Baja — rate limit de login = riesgo demo
5 intentos/min por IP. En el aula, si los 50 salen por la misma IP pública,
solo los primeros 5 logins/min pasan. **Fix demo:** env
`ECOSOLICITUD_CUENTA_INTENTOS` (p.ej. 500) o `server.forward-headers-strategy`
(ya aplicado en prod).

## Cobertura del sim
21 escenarios: accesos rol×ruta, fuzz de registro, FSM de sesión, concurrencia
(registro, aceptar, aceptar×cancelar), idempotencia, XSS, paginación,
aislamiento multi-cuenta, memoria, proxy XFF, seminario.

## Veredicto
El sistema soporta la carga del seminario sin errores de servidor. Antes del
día D: resolver la idempotencia del POST /nueva y subir el límite de login.
