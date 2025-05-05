# EcoSolicitud - Sistema de Gestión de Solicitudes

Aplicación para gestión desarrollada con Spring Boot.

## Tabla de Contenidos
- [Visión General](#visión-general)
- [Características](#características)
- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Requisitos](#requisitos)
- [Instalación y Ejecución](#instalación-y-ejecución)
- [Despliegue](#despliegue)
- [Flujo de Trabajo Git](#flujo-de-trabajo-git)
- [Credenciales por Defecto](#credenciales-por-defecto)

## Visión General
EcoSolicitud es un proyecto de gestión de manera eficiente, con enfoque en la usabilidad y la experiencia del usuario.

## Características Actuales (MVP)
- Sistema de autenticación y autorización básico
- Gestión de usuarios (CRUD)
- Interfaz web con Thymeleaf
- Arquitectura modular con Spring Modulith

## Próximas Características
- Notificaciones por email

## Arquitectura
La arquitectura del sistema sigue el patrón MVC (Modelo-Vista-Controlador) implementado con Spring Modulith, que proporciona:

- **Modularidad**: Organización del código en módulos cohesivos y débilmente acoplados
- **Encapsulamiento**: Cada módulo encapsula su lógica interna y expone APIs bien definidas
- **Independencia**: Los módulos pueden evolucionar de forma independiente
- **Testabilidad**: Facilita pruebas unitarias y de integración por módulo
- **Mantenibilidad**: Mejora la comprensión y mantenimiento del código

Cada módulo contiene sus propios componentes MVC:

- **Modelo**: Entidades JPA que representan los objetos de negocio
- **Vista**: Plantillas Thymeleaf para la interfaz de usuario
- **Controlador**: Controladores Spring que manejan las peticiones HTTP
- **Servicio**: Capa de lógica de negocio
- **Repositorio**: Capa de acceso a datos
- **Módulos**: Implementación de arquitectura modular con Spring Modulith

## Tecnologías
- Java 24 (JDK)
- Spring Boot 3.4.5
- Spring Security
- Spring Data JPA
- Spring Modulith
- Thymeleaf
- H2 Database (desarrollo)
- PostgreSQL (producción)
- Lombok
- Maven
- Docker

## Estructura del Proyecto
```
demo/
├── src/
│   ├── main/
│   │   ├── java/com/ecosolicitud/demo/
│   │   │   ├── config/         # Configuraciones de la aplicación
│   │   │   ├── controller/     # Controladores REST y MVC
│   │   │   ├── model/          # Entidades JPA
│   │   │   ├── repository/     # Interfaces de repositorio
│   │   │   ├── service/        # Servicios para la lógica de negocio
│   │   │   ├── usuarios/       # Módulo de usuarios (arquitectura modular)
│   │   │   │   ├── api/        # API pública del módulo
│   │   │   │   ├── application/ # Casos de uso
│   │   │   │   └── domain/     # Dominio del módulo
│   │   │   └── DemoApplication.java
│   │   └── resources/
│   │       ├── static/         # Recursos estáticos (CSS, JS)
│   │       ├── templates/      # Plantillas Thymeleaf
│   │       └── application.properties
│   └── test/                   # Pruebas unitarias e integración
├── Dockerfile                  # Configuración para Docker
├── pom.xml                     # Configuración de Maven
└── README.md                   # Este archivo
```

## Requisitos
- JDK 24 (o JDK 17 para Docker)
- Maven 3.6+
- PostgreSQL (para producción)
- Docker (opcional, para despliegue)

## Instalación y Ejecución
### Desarrollo Local
1. Clonar el repositorio
   ```bash
   git clone https://github.com/fedegonc/ecosolicitud.git
   cd ecosolicitud
   ```

2. Ejecutar con Maven
   ```bash
   # En Windows
   mvnw.cmd spring-boot:run
   
   # En Linux/Mac
   ./mvnw spring-boot:run
   ```
   
3. Acceder a la aplicación en `http://localhost:8080`

### Base de Datos
- **Desarrollo**: H2 (en memoria) - No requiere configuración adicional
- **Producción**: Configurar PostgreSQL en `application.properties`
  ```properties
  spring.datasource.url=jdbc:postgresql://[host]:[port]/[database]
  spring.datasource.username=[username]
  spring.datasource.password=[password]
  spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
  ```

## Despliegue
### Despliegue con Docker
1. **Construir la imagen Docker**
   ```bash
   docker build -t ecosolicitud-app .
   ```

2. **Ejecutar el contenedor**
   ```bash
   docker run -p 8080:8080 ecosolicitud-app
   ```

### Despliegue en Digital Ocean
#### Requisitos previos
- Cuenta en Digital Ocean
- Digital Ocean CLI instalado (doctl) o usar la interfaz web

#### Pasos para desplegar

1. **Construir la imagen Docker localmente**
   ```bash
   docker build -t ecosolicitud-app .
   ```

2. **Opción 1: Usando Docker Hub como intermediario**
   ```bash
   # Etiqueta la imagen con tu usuario de Docker Hub
   docker tag ecosolicitud-app tuusuario/ecosolicitud-app
   
   # Sube la imagen a Docker Hub
   docker push tuusuario/ecosolicitud-app
   ```

3. **Opción 2: Usando el Container Registry de Digital Ocean**
   ```bash
   # Autenticar con Digital Ocean
   doctl auth init
   
   # Crear un Container Registry si no tienes uno
   doctl registry create mi-registry
   
   # Conectar Docker al registry de Digital Ocean
   doctl registry login
   
   # Etiquetar la imagen para el registry de Digital Ocean
   docker tag ecosolicitud-app registry.digitalocean.com/mi-registry/ecosolicitud-app
   
   # Subir la imagen
   docker push registry.digitalocean.com/mi-registry/ecosolicitud-app
   ```

4. **Crear una App en Digital Ocean**
   - Ve a la consola web de Digital Ocean
   - Selecciona "Apps" y luego "Create App"
   - Elige "Container Registry" y selecciona tu imagen
   - Configura las variables de entorno según sea necesario
   - Selecciona el plan de precios (Basic es suficiente para MVP)
   - Haz clic en "Launch App"

## Flujo de Trabajo Git
El proyecto utiliza el siguiente flujo de trabajo Git:
- **main**: Rama principal del proyecto
- **dev**: Rama de desarrollo donde se implementan nuevas características
- **deploy**: Rama utilizada para despliegue en DigitalOcean

Proceso de desarrollo:
1. Desarrollar nuevas características en la rama `dev`
2. Probar exhaustivamente en la rama `dev`
3. Fusionar cambios de `dev` a `deploy` para despliegue
4. Una vez estable, fusionar a `main`

## Credenciales por Defecto
- Usuario Administrador: admin / password
- Usuario Normal: usuario / password

## Áreas de Mejora
- Implementar autenticación basada en base de datos en lugar de en memoria
- Expandir funcionalidad de gestión de usuarios
- Añadir pruebas unitarias y de integración
- Implementar documentación API con Swagger/OpenAPI
