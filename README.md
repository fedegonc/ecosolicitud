# EcoSolicitud

Aplicación Spring Boot para pedir retiros de reciclables en Rivera y Sant'Ana do Livramento.

## Requisitos

- Java 21
- Maven 3.9+

## Desarrollo

```bash
mvn spring-boot:run
```

## Verificación

```bash
mvn verify
```

## Docker

```bash
docker build -t ecosolicitud .
docker run -p 8080:8080 \
  -e DATABASE_URL=... \
  -e DATABASE_USER=... \
  -e DATABASE_PASSWORD=... \
  ecosolicitud
```
