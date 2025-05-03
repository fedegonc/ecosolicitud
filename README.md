# Sistema de Solicitudes

Aplicación MVP para la gestión de solicitudes desarrollada con Spring Boot.

## Características

- Gestión de usuarios
- Gestión de solicitudes
- Calendario de eventos
- Autenticación y autorización con Spring Security

## Tecnologías

- Java 24
- Spring Boot 3.4.5
- Spring Security
- Spring Data JPA
- Thymeleaf
- PostgreSQL
- Lombok

## Estructura del proyecto

- `model`: Entidades JPA (Usuario, Solicitud, Calendar)
- `repository`: Interfaces de repositorio para acceso a datos
- `service`: Servicios para la lógica de negocio
- `controller`: Controladores para manejar las peticiones HTTP
- `config`: Configuraciones de la aplicación
- `templates`: Plantillas Thymeleaf para las vistas

## Instalación y ejecución

1. Clonar el repositorio
2. Configurar la base de datos PostgreSQL en `application.properties`
3. Ejecutar con Maven: `mvn spring-boot:run`
4. Acceder a la aplicación en `http://localhost:8080`

## Credenciales por defecto

- Usuario: admin / password
- Usuario normal: usuario / password
