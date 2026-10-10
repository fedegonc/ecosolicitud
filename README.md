# EcoSolicitud

Aplicación web municipal para coordinar el retiro de materiales reciclables entre vecinos y centros de acopio. Diseñada para pilotos pequeños (~50 usuarios por instancia) en la frontera Rivera (UY) / Sant'Ana do Livramento (BR), con interfaz bilingüe español–portugués.

[![Verify](https://github.com/fedegonc/ecosolicitud/actions/workflows/ci.yml/badge.svg)](https://github.com/fedegonc/ecosolicitud/actions/workflows/ci.yml)
[![Demo](https://img.shields.io/badge/demo-ecosolicitud.onrender.com-2e7d32)](https://ecosolicitud.onrender.com)

## Funcionalidades

**Vecinos**
- Solicitud de retiro en cascada guiada: ciudad → centro → materiales que ese centro recibe → datos y ubicación en mapa.
- Seguimiento de estados (pendiente → aceptada → completada, rechazada o cancelada), cancelación de pendientes e informe mensual.
- Avisos cuando un centro actúa sobre un pedido, con contador de no leídos.

**Centros de acopio**
- Bandeja kanban (pendientes / en curso / cerradas) con acciones sin recarga de página.
- Perfil público editable: horario, teléfono, materiales recibidos y ubicación en mapa, con vista previa.
- Archivo de cerradas e informe mensual.

**Contenido público**
- Directorio de centros con mapas de OpenStreetMap y horarios.
- Guía de reciclaje con artículos y fotos responsivas.
- Comunidad (novedades e historias) y recicladores independientes.
- Estadísticas reales: aceptación, resolución, tiempo de respuesta y pendientes por centro.
- Cuestionario de opinión en Google Forms con respaldo local por correo.

## Stack

Java 21 · Spring Boot 3.5 · Spring Modulith · Spring MVC + Thymeleaf · Spring Data JPA + H2 · Spring Security · HTMX 2 · Leaflet · PWA (manifest + service worker).

Frontend server-rendered con mejoras progresivas: la app funciona completa sin JavaScript; HTMX agrega swaps parciales, validación por campo y actualización en vivo.

## Arquitectura

Monolito modular. Cada paquete es un módulo con API pública y detalles en `interno`:

```
com.ecosolicitud
├── solicitud      # solicitudes, estados, avisos, métricas
├── organizacion   # centros de acopio, perfil, ubicación
├── comunidad      # publicaciones y recicladores
├── guia           # artículos en Markdown
├── estadisticas   # métricas agregadas
├── opinion        # cuestionario y respaldo por correo
├── demo           # dataset semilla y cambio de rol
└── shared         # tipos y utilidades compartidas (hoja)
```

Las fronteras se verifican en CI (`ModulesTest` + `.github/scripts/arquitectura.sh`): ningún módulo importa el `interno` de otro, `shared` no importa módulos, ningún archivo supera las 200 líneas y los templates no comparan estados del dominio.

## Arranque local

Requisitos: Java 21 y Maven.

```bash
mvn spring-boot:run
# http://localhost:8080/
```

Los datos demo se siembran al primer arranque (H2 en `data/`, ignorado por Git). El bloque "Modo demo" del menú permite cambiar de rol, de centro activo y reiniciar el dataset.

## Pruebas y calidad

```bash
mvn verify                              # suite completa + JaCoCo
bash .github/scripts/arquitectura.sh    # reglas de arquitectura
```

- 64 pruebas: máquina de estados, concurrencia optimista, validaciones sobre el `Validator` real, contratos de templates (HtmlUnit/Selenium) y verificación del artefacto.
- Cobertura JaCoCo: ~85% instrucciones, ~65% ramas.
- CI (`Verify`) en cada push y PR: gate + tests + chequeo de que el jar no incluya herramientas de desarrollo.
- Carga local con JMeter: planes y resultados en [testing/](testing/README.md).

## Despliegue

Docker multi-stage (`Dockerfile`), perfil `prod`. Variables:

| Variable | Propósito |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` en producción |
| `DATABASE_URL` / `DATABASE_USER` / `DATABASE_PASSWORD` | Postgres (Supabase pooler, puerto 5432); sin ellas prod usa H2 en archivo |
| `ecosolicitud.demo.habilitada` | `false` apaga el modo demo |
| `ECOSOLICITUD_DEMO_REINICIAR` | `true` re-habilita el reinicio del dataset (apagado en prod) |
| `ecosolicitud.tema` | nombre de un tema en `static/css/temas/` |
| `MAIL_HOST` / `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP del respaldo del cuestionario |
| `CUESTIONARIO_DESTINO` | casilla que recibe los respaldos |

En producción `/h2-console`, `/actuator/**` y `/livereload.js` responden 404 por diseño.

## Documentación

- [testing/](testing/README.md) — procedimientos de prueba, resultados de carga y reglas de la suite.
