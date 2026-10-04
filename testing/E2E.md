# Pruebas de flujos completos e interfaz

## Alcance y actores

- **Ciudadano:** crea, consulta y cancela solicitudes propias.
- **Representante del centro:** consulta las recibidas y acepta, rechaza o completa; edita el perfil del centro.

La demo permite alternar roles, pero no equivale a autenticación real. Dos navegadores tienen sesiones diferentes y pueden seguir compartiendo la identidad del ciudadano demo.

Las pruebas actuales usan HTTP real contra un puerto aleatorio, `TestRestTemplate` y HtmlUnit mediante la API Selenium. No constituyen una prueba completa de Chrome/Firefox, renderizado visual o ejecución del flujo htmx. No anunciar soporte de PWA o accesibilidad como probado únicamente porque estos tests pasen.

## Automatización existente y resultado inicial

El 2026-10-04 las clases siguientes pasaron en la última verificación funcional. Ejecutar desde la raíz:

```bash
mvn -B -DfailIfNoTests=true \
  -Dtest=ContratosTest,OrganizacionE3Test,SolicitudE4Test,SolicitudE5Test \
  test
```

| Clase | Métodos ejecutados | Comprobaciones principales |
|---|---:|---|
| `ContratosTest` | 1 | Matriz rol/sección, sidebar, portada, rutas inexistentes, rol inválido y CSRF |
| `OrganizacionE3Test` | 1 | Centros por ciudad, selector, perfil válido/inválido, versión vieja y reinicio |
| `SolicitudE4Test` | 4 | Cascada de selección, crear válida/inválida, filtros propios y conservación de materiales |
| `SolicitudE5Test` | 4 | Acciones, autor/destinataria, estados inválidos, versión vieja y botones por estado |
| **Total** | **10** | 0 fallos y 0 errores en la ejecución inicial |

Los métodos tienen varios `Allure.step`; diez ejecuciones no equivalen a diez escenarios independientes. Varias secuencias son largas y dependen de lo realizado antes. La suite usa datos ficticios y reinicio del dataset en H2 en memoria, nunca el reinicio remoto.

## Matriz de trazabilidad y aceptación

| ID | Caso de uso o regla | Resultado esperado | Evidencia / estado |
|---|---|---|---|
| E-01 | Elegir ciudad y centro | Solo centros de esa ciudad y materiales que recibe el centro | `SolicitudE4Test.cascada`, ejecutado |
| E-02 | Crear solicitud válida | Una fila pendiente, fecha de creación y confirmación con centro destinatario | `SolicitudE4Test.crear`, ejecutado |
| E-03 | Enviar formulario inválido | Error útil, valores conservados y ninguna solicitud nueva | `SolicitudE4Test.crear`, ejecutado para escenarios incluidos |
| E-04 | Consultar propias | Solo solicitudes del ciudadano, orden y filtros correctos | `SolicitudE4Test.lista`, ejecutado |
| E-05 | Aceptar/rechazar/completar | Transición permitida y mensaje; prohibir estado o destinataria incorrectos | `SolicitudE5Test.accionesOrganizacion`, ejecutado |
| E-06 | Cancelar | Solo propia pendiente; finalización coherente | `SolicitudE5Test.accionesCiudadano`, ejecutado |
| E-07 | Bandeja del centro | Solo solicitudes destinatarias, grupos y botones válidos | `SolicitudE5Test.bandeja`, ejecutado |
| E-08 | Editar perfil | Guardado válido; inválido o versión vieja no sobrescribe | `OrganizacionE3Test.e3`, ejecutado |
| E-09 | Recibir aviso por acción | Destinatario correcto, un aviso por cambio efectivo y lectura coherente | Pendiente de evidencia funcional explícita; no inferirlo de tests de estados |
| E-10 | Navegación con roles distintos en dos sesiones | Cambiar rol en una sesión no modifica la otra | Pendiente de corregir la comprobación actual, que termina comparando dos ciudadanos |
| E-11 | Estadísticas históricas | Una aceptada y después rechazada sigue contando como aceptada si esa es la definición de negocio | Pendiente; cálculo actual solo cuenta EN_CURSO/COMPLETADA |
| E-12 | Opinión | Secciones válidas y valores 1..5; comentario excesivo produce error útil y no un 500 | Pendiente de evidencia HTTP específica; @Size en entidad no resuelve por sí sola el mensaje |

Registrar por separado una regla no cubierta aunque otras partes de la misma página tengan tests. No equiparar un mensaje de éxito con integridad de datos si no se verificó el estado persistido.

## Campaña manual en navegador real: pendiente

Ejecutar con datos sintéticos en una instancia aislada o coordinada. No usar el reinicio remoto sin autorización específica. Anotar navegador, versión, viewport, idioma, fecha y commit.

| ID | Procedimiento | Aceptación |
|---|---|---|
| M-01 | Completar ciudad → centro → materiales con htmx | Sin perder contacto/dirección al cambiar selección, sin errores de consola |
| M-02 | Recorrido ciudadano → centro → ciudadano | Crear, aceptar, completar y consultar estado/aviso coherentes |
| M-03 | Navegar solo con teclado | Foco visible, acceso al menú y labels, sin bloqueo del flujo |
| M-04 | Viewports 360 px y escritorio | No ocultar acciones ni provocar scroll horizontal en formularios; comprobar tabla Likert |
| M-05 | Cambiar ES/PT en páginas y formularios | Textos, errores y navegación coherentes; registrar pérdidas de parámetros/datos |
| M-06 | Desactivar JavaScript | Continuar mediante GET y completar el formulario donde se ofrece fallback |
| M-07 | Instalar PWA y abrirla | Instalación y navegación cuando el navegador soporte el mecanismo |
| M-08 | Perder conectividad y recuperarla | Mensaje comprensible, sin presentar solicitudes como enviadas si no hubo confirmación |
| M-09 | Doble clic y recarga después de POST | Registrar si crea duplicados; no asumir idempotencia por el redirect |
| M-10 | Dos identidades ficticias distintas | Aislamiento de solicitudes y avisos; bloqueado en el piloto autónomo hasta asignar identidades distintas |

El SW es network-only: no se promete operación offline. Los colores, contraste y temas deben comprobarse en navegador; HtmlUnit emite advertencias sobre CSS moderno y no sustituye esa revisión.

## Consolidación propuesta, no aplicada

- Organizar nombres por comportamiento, no solamente por etapa E3/E4/E5.
- Extraer una ayuda pequeña de sesión/CSRF si elimina repetición de los tres usos reales existentes.
- Separar los recorridos que dependen de un paso previo para que un fallo no impida verificar casos posteriores.
- Mantener integraciones que prueban permisos y persistencia: una matriz unitaria de estados no demuestra esos puntos.
- Corregir la prueba de dos sesiones usando roles diferentes y una modificación en solo una.
- Añadir únicamente las regresiones de avisos, estadísticas o conflictos cuando exista la regla o el bug reproducido; no ampliar la suite por cantidad.
