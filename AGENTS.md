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
- Perfiles: `dev` por defecto (loopback 127.0.0.1, Thymeleaf sin caché, DevTools) y `prod` (0.0.0.0, caché activado) seleccionado vía `SPRING_PROFILES_ACTIVE=prod`; el puerto sale de `${PORT:8080}`.
- Deploy: `Dockerfile` multi-stage (Maven → JRE 21) corriendo perfil `prod`; Render despliega la rama `main`.
- Imágenes y recursos estáticos en `src/main/resources/static/` (`img/`, `css/`); un solo `app.css` mobile-first con tokens CSS.
- Documentación interna o de trabajo (notas, `CLAUDE.md`, archivos locales) va al `.gitignore`: el repo público se mantiene limpio con lo esencial.
- Tests E2E con Selenium `HtmlUnitDriver` (sin JS, headless); reporte Allure en `target/allure-results`, generar HTML con `mvn allure:report` → `target/site/allure-maven-plugin/index.html`.
- `Seccion` es la única lista de secciones: clave i18n, ruta, rol e ícono. La sidebar se arma desde ahí; `NavegacionAdvice` agrega `rutaActual` y `secciones` (filtradas por rol) al modelo.

## Esquema y datos de demo

- Los datos de la demo son desechables: la fuente de verdad es `DemoService`, que resiembra al arrancar si la base está vacía.
- `ddl-auto=create`: el esquema se recrea en cada arranque y `DemoService` resiembra porque la base queda vacía. No hay deriva de esquema posible y no hace falta borrar `data/` a mano.
- Consecuencia asumida: lo que se cargue durante una sesión se pierde en el próximo reinicio. Es el contrato del modo demo, no un defecto.
- Por qué no `update`: no altera tablas de colección (`@ElementCollection`) ni elimina columnas que ya no se mapean; la app queda leyendo un esquema a medias.
- No editar el esquema a mano con el Shell de H2 esperando que persista: el DDL se confirma solo, el DML necesita `COMMIT;` explícito.
- `spring-boot:run` corre con `addResources=true`: los cambios en `src/main/resources` (templates, CSS, JS) se sirven en vivo sin recompilar. Los cambios de Java sí requieren recompilar.
- Cuando los datos empiecen a importar (primer usuario real del beta), el reemplazo de `create` es Flyway con `ddl-auto=validate`.
