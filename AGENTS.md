# Proyecto

- Java 21 y Maven; Spring Boot, Thymeleaf, Layout Dialect y H2.
- Verificación: `mvn verify`.
- Desarrollo: `mvn spring-boot:run`. Usar este modo para mantener la app funcionando mientras se ejecuta `mvn verify`.
- Ejecutable: `java -jar target/ecosolicitud-0.0.1-SNAPSHOT.jar`. No ejecutar `mvn verify` mientras se usa ese mismo JAR: el reempaquetado reemplaza el archivo y puede romper la carga de clases del proceso activo.
- La página inicial está en `http://localhost:8080/`, sin estilos visuales por ahora.
- H2 persiste en `data/ecosolicitud` relativo al directorio de ejecución. Ejecutar desde la raíz del proyecto.
- Los tests de contexto deben llevar `@ActiveProfiles("test")`: `src/test/resources/application-test.properties` configura H2 en memoria sin modificar la base local.
- Todo el código Java pertenece al paquete base `com.ecosolicitud`.
- Spring Modulith 1.4.x es compatible con Spring Boot 3.5.x; `ModulesTest` verifica las fronteras con `ApplicationModules.verify()`.
- GitHub Actions ejecuta `mvn -B verify` con Java 21 en cada push y PR; el estado remoto solo se puede verificar una vez publicado el proyecto.
- Consola H2 desactivada por defecto; Hibernate usa `update` solo para la fase de demo, con migraciones pendientes cuando se estabilice el modelo.
- Escucha únicamente en loopback (127.0.0.1).
- Documentación interna o de trabajo (notas, `CLAUDE.md`, archivos locales) va al `.gitignore`: el repo público se mantiene limpio con lo esencial.
- Tests E2E con Selenium `HtmlUnitDriver` (sin JS, headless); reporte Allure en `target/allure-results`, generar HTML con `mvn allure:report` → `target/site/allure-maven-plugin/index.html`.
- `Seccion` es la única lista de secciones: clave i18n, ruta, rol e ícono. La sidebar se arma desde ahí; `NavegacionAdvice` agrega `rutaActual` y `secciones` (filtradas por rol) al modelo.
