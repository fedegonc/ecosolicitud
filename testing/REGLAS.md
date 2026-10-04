# Reglas, pruebas de dominio y regresiones

## Conteo honesto de la suite

Inventario inicial del 2026-10-04: 11 archivos Java, 12 clases y 23 métodos; 78 ejecuciones JUnit. La cifra se descompone en 19 tests normales, 20 combinaciones de transición, 17 teléfonos en el formulario, los mismos 17 en la entidad y cinco direcciones.

El último `mvn verify` conocido antes de publicar estos documentos pasó con cero fallos, errores y omitidos. No garantiza cobertura completa del negocio. JMeter no forma parte de esas 78 ejecuciones.

## Pruebas de dominio y validación

```bash
mvn -B -DfailIfNoTests=true \
  -Dtest=SolicitudMaquinaTest,ValidacionesTest test
```

| ID | Regla | Prueba existente | Resultado inicial |
|---|---|---|---|
| R-01 | Aceptar solo PENDIENTE | Matriz de `SolicitudMaquinaTest` | Pasó |
| R-02 | Rechazar PENDIENTE o EN_CURSO | Misma matriz | Pasó |
| R-03 | Completar solo EN_CURSO | Misma matriz | Pasó |
| R-04 | Cancelar solo PENDIENTE | Misma matriz; propiedad se verifica en integración | Pasó |
| R-05 | Una transición inválida no cambia estado ni fecha final | Misma matriz | Pasó para assertions incluidas |
| R-06 | Los estados finales no admiten acciones | Matriz y `finalesCerradas` | Pasó; existe redundancia |
| R-07 | Construcción rechaza finalización inconsistente o anterior a creación | `invariantesEnConstruccion` | Pasó para dos casos existentes |
| R-08 | Teléfono acepta formatos reales y rechaza incompletos/basura | 17 entradas contra `PerfilForm` | Pasó |
| R-09 | Entidad y formulario tienen regla de teléfono compatible | Mismas 17 entradas contra `Organizacion` con Validator | Pasó; no demuestra por sí sola persistencia por el servicio |
| R-10 | Dirección no vacía y máximo 120 caracteres | Cinco entradas contra `NuevaForm` | Pasó para la propiedad dirección |

Las validaciones se ejecutan con el `Validator` real, no contra una copia del regex. Algunos casos inspeccionan violaciones de una propiedad; no declaran válido todo el formulario. Probar formato y probar persistencia son garantías distintas.

## Contratos técnicos y entornos

| Clase | Ejecuciones | Propósito y limitación |
|---|---:|---|
| `ModulesTest` | 1 | Modulith verifica fronteras de módulos |
| `DatasetTest` | 1 | El runner no reemplaza el perfil modificado si ya existen organizaciones; no es un reinicio real del proceso |
| `DemoDeshabilitadoTest` | 1 | Apagar demo elimina reinicio y botón; no comprueba ni desactiva cambio de rol/organización |
| `EcoSolicitudApplicationTests` | 2 | Contexto con H2 en memoria y ausencia de consola H2 |
| `CasosDeUsoDevTest` | 1 | Catálogo JSON con actores, casos y relaciones en dev |
| `CasosDeUsoProdTest` | 1 | Endpoint del catálogo ausente en prod, con H2 de test en memoria |

El controller del catálogo tiene `@Profile("dev & !prod")`. La combinación simultánea dev/prod se excluye por esa expresión, pero todavía no hay un test separado de ese perfil combinado.

El gate [arquitectura.sh](../.github/scripts/arquitectura.sh) pasó. Tiene siete reglas estructurales/de vistas/tema. Su listado de módulos para las fronteras todavía está incompleto respecto de todos los módulos actuales: pendiente de ampliar, sin confundir el mensaje OK con cobertura total del script. Modulith sí se ejecuta como test local y, al versionar `src/test`, también estará disponible en el checkout de CI.

## Prioridades de regresión y soluciones anticipadas

Estas son propuestas; no están implementadas por crear este documento.

| Prioridad | Riesgo o bug | Prueba que lo demuestra | Solución mínima a evaluar |
|---|---|---|---|
| P0 para piloto autónomo | Todos los ciudadanos demo comparten identidad | Dos ciudadanos ficticios crean y consultan sin ver ni cancelar pedidos ajenos | Identidad ficticia distinta por participante; autenticar antes de datos reales |
| P0 para piloto compartido | Reinicio disponible para cualquier visitante demo | Usuario no facilitador no puede ejecutar reinicio | Reservar reinicio y coordinar dataset; no añadir otro endpoint destructivo |
| P1 | `demo.habilitada=false` no apaga rol/organización ni seed automático | Perfiles sin demo: endpoints ausentes y base vacía sin contenido ficticio | Condicionar todos los mecanismos de demo y ocultar sus controles |
| P1 | Conflicto real puede terminar en error al commit | Dos transacciones simultáneas: una gana y otra informa conflicto, sin cambio/aviso duplicado | Tratar excepción fuera de la transacción fallida si se reproduce; no capturar y continuar en transacción rollback-only |
| P1 | Aceptada luego rechazada no cuenta en aceptación histórica | Crear → aceptar → rechazar y verificar la definición acordada de aceptación | Campo `aceptadaEn` si se mantiene la definición histórica; no hace falta event sourcing |
| P1 | Comentario excesivo puede producir error HTTP en vez de validación útil | POST con 501 caracteres y valoración válida | Validar en entrada y devolver mensaje; conservar constraint de persistencia |
| P1 | Opiniones sin sección real no deben alterar agregados | Enviar clave inexistente y comparar filas/promedio antes y después | Whitelist ya implementada; falta prueba explícita de regresión |
| P2 | Repetición innecesaria de la matriz de teléfonos | Comprobar inválido a través del servicio y flush en base aislada | Conservar una matriz y sustituir la segunda por integración enfocada |
| P2 | Doble cobertura de estados finales | Identificar las mismas combinaciones en la matriz y el método extra | Retirar redundancia solo después de comprobar assertions equivalentes |
| P2 | Aislamiento de sesión insuficientemente demostrado | Mantener roles distintos en dos navegadores y cambiar solo uno | Corregir `ContratosTest`, no aumentar contador por maquillaje |

No agregar todas las pruebas de esta tabla automáticamente. Priorizar el circuito del piloto y escribir una regresión pequeña cuando se acuerde la regla o se reproduzca el problema.

## Seguridad y empaquetado

- CSRF permanece activo y tiene comprobaciones HTTP. `permitAll` y cambio libre de rol son parte de la demo, no autenticación multiusuario.
- Los tests de contexto conservan `@ActiveProfiles("test")`; dev/prod se combinan con `test` para utilizar H2 en memoria en esos escenarios.
- No publicar secrets, bases, cookies, resultados con datos personales ni logs de la app.
- Maven separa `src/test` de `src/main` y las dependencias de test no se incluyen en el jar por publicarlas en Git.
- CI ejecuta arquitectura, `mvn -B -DfailIfNoTests=true verify` y rechazo de bibliotecas de desarrollo dentro del jar.
- Docker mantiene un build sin ejecutar tests; CI es la verificación previa. Un deploy no demuestra por sí solo que CI terminó correctamente.

## Cómo evaluar una falla

1. Registrar comando, entorno, commit y assertion que falla.
2. Reproducir con el mínimo escenario, sin tocar la base compartida.
3. Distinguir regla incorrecta, implementación incorrecta, fixture incorrecto y problema de infraestructura.
4. No ampliar el regex para aceptar basura ni quitar CSRF o controles de arquitectura para que el build pase.
5. Corregir la causa y volver a ejecutar la prueba enfocada y luego la suite.
6. Actualizar el resultado de este documento; una corrección no probada sigue pendiente.

Los fallos por infraestructura no se disfrazan como éxito. La decisión de retirar o reemplazar una prueba debe preservar la garantía que intentaba verificar y quedar explicada.
