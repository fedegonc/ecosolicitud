Arranque (Java 21 y Maven): desde la raíz, ejecutar `mvn spring-boot:run` y abrir http://localhost:8080/.
Datos: H2 en archivo `data/ecosolicitud.mv.db`, fuera de `target/` e ignorado por Git; consola H2 desactivada y esquema `update` durante la demo.
Pruebas: `mvn -B -DfailIfNoTests=true verify` usa H2 en memoria con el perfil `test`, verifica fronteras con Modulith y corre también en GitHub Actions en cada push y PR.

El código de pruebas y el plan JMeter son públicos en [src/test/](src/test/); no entran al jar de producción. Procedimientos, resultados, reglas y pendientes: [testing/](testing/README.md). La carga remota se ejecuta manualmente, no desde CI.
