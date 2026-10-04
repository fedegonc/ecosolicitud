# Pruebas de EcoSolicitud

Este directorio concentra procedimientos, reglas de aceptación, resultados y pendientes. El alcance es un proyecto de Tecnólogo en Análisis y Desarrollo de Sistemas con un piloto de datos ficticios, no una certificación de disponibilidad empresarial.

## Índice

| Documento | Contenido |
|---|---|
| [Carga](CARGA.md) | JMeter, resultados locales y en Render, interpretación de capacidad, escalones y criterios de parada |
| [E2E](E2E.md) | Flujos de los dos actores, pruebas HTTP/HtmlUnit y comprobaciones manuales en navegador |
| [Reglas y regresiones](REGLAS.md) | Validaciones, estados, permisos, arquitectura, perfiles, redundancias y riesgos pendientes |
| [Resultados agregados del 2026-10-04](resultados/2026-10-04.json) | Evidencia resumida sin cookies, credenciales ni datos personales |

Los planes y el código ejecutable están en [src/test](../src/test/). Son parte del repositorio público, pero Maven no los incluye en el jar de producción. Las dependencias de pruebas conservan su scope `test`.

## Ejecución funcional

Requisitos: Java 21 y Maven. Desde la raíz:

```bash
bash .github/scripts/arquitectura.sh
mvn -B -DfailIfNoTests=true verify
```

Los tests de contexto usan el perfil `test` y H2 en memoria. No necesitan que esté levantada la app del puerto 8080 y no deben tocar `data/ecosolicitud`.

Salidas locales, excluidas de Git:

- `target/surefire-reports/`: resultados JUnit.
- `target/site/jacoco/index.html`: cobertura; no es garantía de corrección.
- `target/allure-results/`: resultados para Allure, si se utiliza.
- JTL, logs y reportes HTML de JMeter bajo `target/`.

JMeter se ejecuta por separado: `mvn verify` no ejecuta el plan de carga ni contacta a Render. La carga remota es manual y requiere autorización para ese entorno.

## Evidencia funcional inicial

El 2026-10-04 se verificaron 11 archivos Java, 12 clases y 23 métodos de prueba. JUnit informó 78 ejecuciones: 19 métodos normales y cuatro métodos parametrizados que producen 59 ejecuciones. Resultado: 0 fallos, 0 errores y 0 omitidos. El gate de arquitectura también pasó.

El contador no constituye una meta. Una matriz de datos puede generar varias ejecuciones; un método largo puede esconder muchos escenarios bajo una sola ejecución. La calidad se evalúa por trazabilidad con requisitos, independencia y capacidad de detectar regresiones.

## Estados y registro de resultados

- **Ejecutado:** tiene comando o procedimiento, entorno, fecha y evidencia identificable.
- **Pendiente:** está propuesto, pero no se ejecutó o no tiene evidencia suficiente.
- **Hipótesis:** explicación o estimación que todavía requiere medición.
- **Bloqueado:** requiere aislamiento, permisos, configuración o una corrección previa.

Para cada ejecución registrar:

```text
ID / fecha y zona horaria:
Objetivo / requisito:
Commit probado / cambios sin commit:
Entorno / perfil / plan / tamaño del dataset:
Herramienta / versión / parámetros:
Resultado esperado:
Resultado observado / errores / tiempos:
Evidencia sin datos sensibles:
Conclusión limitada al escenario:
Pendientes / decisión:
```

No reutilizar informes antiguos de `target/surefire-reports` como prueba de una ejecución nueva. Si se necesita `mvn clean`, recordar que elimina también las evidencias locales y cualquier instalación de JMeter dentro de `target/`.

## Reglas de trabajo

1. Usar datos sintéticos. No publicar respuestas HTML, cookies de sesión, tokens CSRF, credenciales ni logs sin revisar.
2. Vincular una prueba a un requisito, riesgo o regresión; no agregarla para subir el contador.
3. No eliminar una prueba útil solamente para reducir ese contador. Consolidar duplicación real.
4. Distinguir lógica del dominio, integración HTTP, interfaz real y rendimiento.
5. No confundir sesiones independientes con ciudadanos distintos: hoy la identidad de ciudadano demo es compartida.
6. No ejecutar reinicios, borrados ni carga de escrituras sobre el servicio compartido sin autorización específica.
7. No desplegar ni cambiar de plan como parte de una prueba. Render despliega actualmente desde `main`; verificar el commit activo antes de comparar resultados.
8. Cada riesgo descubierto se registra en estos documentos con estado y próximo paso. No etiquetar un pendiente como resuelto porque el build esté verde.
