# Despliegue en Digital Ocean

## Requisitos previos
- Cuenta en Digital Ocean
- Digital Ocean CLI instalado (doctl) o usar la interfaz web

## Pasos para desplegar

### 1. Construir la imagen Docker localmente
```bash
docker build -t ecosolicitud-app .
```

### 2. Opción 1: Usando Docker Hub como intermediario
```bash
# Etiqueta la imagen con tu usuario de Docker Hub
docker tag ecosolicitud-app tuusuario/ecosolicitud-app

# Sube la imagen a Docker Hub
docker push tuusuario/ecosolicitud-app
```

### 3. Opción 2: Usando el Container Registry de Digital Ocean
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

### 4. Crear una App en Digital Ocean
- Ve a la consola web de Digital Ocean
- Selecciona "Apps" y luego "Create App"
- Elige "Container Registry" y selecciona tu imagen
- Configura las variables de entorno según sea necesario
- Selecciona el plan de precios (Basic es suficiente para MVP)
- Haz clic en "Launch App"

### 5. Alternativa: Usar Droplet con Docker
```bash
# Crea un Droplet con Docker preinstalado desde la web de Digital Ocean
# Conéctate al Droplet vía SSH

# Clona este repositorio
git clone [URL_DEL_REPO]
cd [NOMBRE_DEL_REPO]

# Despliega usando docker-compose
docker-compose up -d
```
