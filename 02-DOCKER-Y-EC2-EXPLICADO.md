# Docker, Dockerfile, puertos, redes y EC2 — EXPLICADO desde cero

> Objetivo: entender **de verdad** qué pasa cuando levantas los contenedores de tu práctica.
> Basado en tu diagrama (`pmng-net` con 3 contenedores) y tu proyecto `practica-3-people-crud`.
> Esta guía responde tus dudas concretas:
> - ¿Cómo subo archivos a la EC2?
> - ¿Cómo se estructura un Dockerfile?
> - ¿Puedo copiar/pegar el mismo Dockerfile para todos los proyectos o solo cambio el puerto?
> - ¿Qué son las redes y los puertos?

---

## 1. Las 4 palabras de Docker que DEBES entender

| Palabra | Analogía | Qué es |
|---------|----------|--------|
| **Imagen** | La receta + ingredientes empaquetados | Un paquete inmóvil con tu app ya lista para correr |
| **Contenedor** | El plato ya cocinado y servido | Una imagen **en ejecución** (puedes tener varios de la misma imagen) |
| **Dockerfile** | La receta escrita paso a paso | El archivo de texto que dice cómo construir la imagen |
| **Red (network)** | El comedor donde los platos se pasan cosas | Una red privada donde los contenedores se hablan por nombre |

**Flujo general:**
```
Dockerfile  --(docker build)-->  Imagen  --(docker run)-->  Contenedor corriendo
```

- `docker build` lee el **Dockerfile** y produce una **imagen**.
- `docker run` toma una **imagen** y la pone a correr como **contenedor**.

---

## 2. Los puertos: la duda #1 de todos

Hay **DOS puertos** en juego y es la mayor fuente de confusión.

```
   -p 8089:8080
      │    │
      │    └── Puerto INTERNO del contenedor (donde escucha la app adentro)
      └─────── Puerto EXTERNO / del host EC2 (por donde entras desde afuera)
```

Regla: **`-p EXTERNO:INTERNO`**.

### En tu práctica (del diagrama):

| Contenedor | Puerto interno (la app) | Puerto externo (publicado en EC2) | Quién lo usa |
|------------|------------------------|-----------------------------------|--------------|
| pmng-mysql | 3306 | 3306 (o ninguno) | Solo el back, por la red interna |
| pmng-back (Spring) | 8080 | **8089** | El navegador/Postman: `ip_publica:8089` |
| pmng-front (Nginx) | 80 | **5173** | El navegador: `ip_publica:5173` |

**¿Por qué el interno del back es 8080 pero afuera es 8089?**
Porque dentro del contenedor Spring Boot siempre escucha en 8080 (su `server.port`). Pero tú decides en qué puerto de la EC2 exponerlo al mundo. Tu profe eligió 8089 afuera. Podría ser cualquier otro.

**¿Por qué MySQL casi no necesita puerto externo?**
Porque nadie desde internet le habla directo. Solo el backend le habla, y lo hace **por la red interna de Docker** (ver sección 4), no por el puerto público. Solo abres el 3306 si quieres conectarte con MySQL Workbench desde tu PC.

---

## 3. Anatomía de un Dockerfile (explicado línea por línea)

Hay dos tipos de Dockerfile en tu proyecto. Son **diferentes** porque hacen cosas distintas.

### 3.A — Dockerfile del BACKEND (Spring Boot) — *multi-stage*

Tu backend es Java. Hay que **compilarlo** (convertir `.java` en un `.jar` ejecutable) y luego **ejecutarlo**. Por eso usa 2 etapas ("multi-stage build"):

```dockerfile
# ===== ETAPA 1: BUILD (compilar) =====
FROM maven:3.9-eclipse-temurin-21 AS build    # imagen con Maven + Java para compilar
WORKDIR /app                                  # carpeta de trabajo dentro del contenedor

COPY pom.xml .                                # copia primero el pom (para cachear dependencias)
RUN mvn dependency:go-offline -B              # descarga dependencias (se cachea si pom no cambia)

COPY src ./src                                # ahora copia tu código fuente
RUN mvn clean package -DskipTests             # compila y genera el .jar en /app/target/

# ===== ETAPA 2: RUNTIME (ejecutar) =====
FROM eclipse-temurin:21-jre                   # imagen ligera, SOLO Java (sin Maven)
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar   # copia el .jar de la etapa anterior
EXPOSE 8080                                    # documenta que la app usa el 8080 (informativo)
ENTRYPOINT ["java", "-jar", "app.jar"]         # comando que arranca la app
```

**¿Por qué dos etapas?** Para que la imagen final sea pequeña. La etapa 1 (con Maven, que pesa mucho) se descarta; solo te quedas con el `.jar` + Java para ejecutarlo.

**Instrucciones clave de un Dockerfile:**

| Instrucción | Qué hace |
|-------------|----------|
| `FROM` | De qué imagen base partes (Java, Node, Nginx...) |
| `WORKDIR` | En qué carpeta trabajar dentro del contenedor |
| `COPY` | Copia archivos de tu PC → al contenedor |
| `RUN` | Ejecuta un comando **al construir** la imagen (compilar, instalar) |
| `EXPOSE` | Documenta el puerto interno (no lo abre solo; es informativo) |
| `ENTRYPOINT` / `CMD` | El comando que corre **al arrancar** el contenedor |

> Ojo: en tu proyecto el archivo del back se llama `DockerFile` (con F mayúscula). Por eso al construir usas `-f DockerFile`. Lo normal es llamarlo `Dockerfile`.

### 3.B — Dockerfile del FRONTEND (HTML/JS con Nginx)

Tu front son archivos estáticos (`index.html`, `app.js`). No se compilan. Solo hay que **servirlos** con un servidor web (Nginx):

```dockerfile
FROM nginx:alpine                                   # servidor web Nginx, versión ligera
COPY index.html /usr/share/nginx/html/index.html    # copia tu HTML a donde Nginx sirve
COPY app.js /usr/share/nginx/html/app.js            # copia tu JS
EXPOSE 80                                            # Nginx sirve en el puerto 80
# No necesita ENTRYPOINT: la imagen de Nginx ya arranca Nginx sola
```

Mucho más simple porque no hay que compilar nada.

---

## 4. Respuesta directa: ¿copio/pego el mismo Dockerfile para todos?

**Respuesta corta:** NO es que solo cambies el puerto. Depende del **tipo de proyecto**. Pero SÍ hay plantillas reutilizables por tecnología.

### Regla práctica:

| Si tu proyecto es... | Usa esta plantilla de Dockerfile | ¿Qué cambias? |
|----------------------|----------------------------------|---------------|
| **Spring Boot (Java/Maven)** | El multi-stage de la sección 3.A | Casi NADA. Quizá la versión de Java (`21`→`17`). El `EXPOSE` puede quedarse en 8080 siempre |
| **Front estático (HTML/JS/CSS)** | El de Nginx de la sección 3.B | Los nombres de archivos que copias |
| **Front moderno (React/Next build)** | Multi-stage Node + Nginx (ver abajo) | Casi nada |

**Entonces, para OTRO CRUD de Spring Boot:** sí, puedes **copiar y pegar el mismo Dockerfile del backend tal cual**. No necesitas ni cambiar el puerto interno (déjalo en 8080 siempre). Lo que cambias es el **puerto externo**, pero eso NO está en el Dockerfile: está en el comando `docker run -p EXTERNO:8080`.

> 🔑 **Idea clave:** el puerto externo NO se decide en el Dockerfile. Se decide al correr el contenedor con `-p`.
> Por eso el mismo Dockerfile sirve para muchos proyectos: la diferencia de puertos la pones al hacer `docker run`.

### (Extra) Plantilla para un front React/Next que SÍ se compila:

```dockerfile
# Etapa 1: build del front
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build          # genera la carpeta dist/ o build/

# Etapa 2: servir con Nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html   # ojo: Next/Vite usan dist o out
EXPOSE 80
```

---

## 5. Las redes de Docker (lo que más te confunde del diagrama)

En tu diagrama todos los contenedores están sobre una barra azul que dice **`pmng-net`**. Eso es una **red privada de Docker**.

**¿Para qué sirve?** Para que los contenedores se hablen entre sí **por su nombre**, sin usar IPs.

Mira estas dos líneas del diagrama:

```
Desde FUERA (navegador):   jdbc:mysql://ip_publica:3306/people-dev
Desde DENTRO (el back):    jdbc:mysql://pmng-mysql:3306/people-dev
                                        ^^^^^^^^^^
                                        el NOMBRE del contenedor, no una IP
```

**La magia:** cuando dos contenedores están en la misma red (`--network pmng-net`), Docker les pone un "DNS interno". El backend puede escribir `pmng-mysql` como si fuera un dominio, y Docker lo resuelve al contenedor de MySQL. **Por eso no usas `localhost` entre contenedores** — cada contenedor tiene su propio `localhost`.

Resumen de cómo se habla cada quién:

```
Navegador (tu PC, FUERA de EC2)  --->  ip_publica:5173   (front)
Navegador (tu PC, FUERA de EC2)  --->  ip_publica:8089   (back)
Contenedor back (DENTRO de EC2)  --->  pmng-mysql:3306   (por la red pmng-net)
```

Regla de oro:
- **Fuera de la EC2** (tu navegador) → usas **IP pública + puerto publicado**.
- **Entre contenedores** (back↔mysql) → usas **nombre del contenedor + puerto interno**.

---

## 6. ¿Cómo subo mis archivos a la EC2? (tu duda principal)

Tienes 3 opciones. La de tu práctica es la #1 (scp).

### Opción 1 — `scp` (copiar archivos por SSH) — la de tu práctica

`scp` = "secure copy". Copia archivos de tu PC a la EC2 usando tu llave `.pem`.

```powershell
# 1. Comprime cada carpeta en un .zip (cliente.zip y people-service.zip)

# 2. Desde PowerShell, en la carpeta donde están los .zip y el .pem:
scp -i "mi-llave.pem" cliente.zip ec2-user@IP_PUBLICA:/home/ec2-user/
scp -i "mi-llave.pem" people-service.zip ec2-user@IP_PUBLICA:/home/ec2-user/

# 3. Entra por SSH a la EC2:
ssh -i "mi-llave.pem" ec2-user@IP_PUBLICA

# 4. Ya dentro de la EC2, descomprime:
unzip cliente.zip -d cliente
unzip people-service.zip -d people-service
```

> Si tu EC2 es **Ubuntu**, el usuario es `ubuntu` en vez de `ec2-user`.
> `IP_PUBLICA` es la IP pública de tu instancia (la ves en la consola de AWS).

**Estructura de `scp`:** `scp -i LLAVE   ARCHIVO_LOCAL   USUARIO@IP:/ruta/destino/`

### Opción 2 — `git clone` (si tu proyecto está en GitHub)
Dentro de la EC2 (por SSH): `git clone https://github.com/tu-usuario/tu-repo.git`
Más limpio que andar comprimiendo zips.

### Opción 3 — Docker Hub (subir la imagen ya construida)
Construyes la imagen en tu PC, la subes a Docker Hub con `docker push`, y en la EC2 solo haces `docker pull`. Más avanzado; por ahora quédate con scp o git.

---

## 7. Receta COMPLETA de despliegue en la EC2 (los comandos en orden)

> Asume: EC2 ya tiene Docker y MySQL instalados (como dices). IP_PUBLICA = la de tu instancia.

### Paso 0 — Antes de subir: ajusta la URL del front
En `cliente/app.js`, el navegador corre FUERA de la EC2, así que la URL del back debe ser pública:
```js
const url = "http://IP_PUBLICA:8089/people";   // NO localhost
```

### Paso 1 — Abrir puertos en el Security Group (consola de AWS)
EC2 → Security Groups → tu grupo → Edit inbound rules → agrega (Custom TCP):
```
Puerto 22    -> SSH       (Source: My IP)
Puerto 8089  -> Backend   (Source: 0.0.0.0/0)
Puerto 5173  -> Frontend  (Source: 0.0.0.0/0)
```
El 3306 NO hace falta abrirlo al exterior (el back usa la red interna).

> El **Security Group** es el "firewall" de AWS. Aunque Docker publique un puerto, si el Security Group no lo abre, desde internet no entra nada. Son dos puertas que deben estar abiertas.

### Paso 2 — Crear la red
```bash
docker network create pmng-net
```

### Paso 3 — Levantar MySQL
```bash
docker run -d \
  --name pmng-mysql \
  --network pmng-net \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=people-dev \
  -p 3306:3306 \
  mysql:8.0
```
Espera 20-30 s a que inicie. Verifica: `docker ps`

### Paso 4 — Construir y levantar el BACKEND
```bash
cd /home/ec2-user/people-service
docker build -f DockerFile -t backend:v1.0 .     # ojo: -f DockerFile por la F mayúscula

docker run -d \
  --name pmng-back \
  --network pmng-net \
  -e DB_URL="jdbc:mysql://pmng-mysql:3306/people-dev?createDatabaseIfNotExist=true&serverTimezone=UTC" \
  -e DB_USER=root \
  -e DB_PASS=root \
  -p 8089:8080 \
  backend:v1.0

docker logs -f pmng-back    # espera a ver "Started PeopleServiceApplication", luego Ctrl+C
```

> Fíjate cómo las variables `-e DB_URL=...` reemplazan los valores del `application.properties`
> (recuerda el `${DB_URL:...}` de la guía 01). Por eso el `DB_URL` apunta a `pmng-mysql` (nombre del contenedor).

### Paso 5 — Construir y levantar el FRONTEND
```bash
cd /home/ec2-user/cliente
docker build -t frontend:v1.0 .

docker run -d \
  --name pmng-front \
  --network pmng-net \
  -p 5173:80 \
  frontend:v1.0
```

### Paso 6 — Verificar y probar
```bash
docker ps     # deben salir los 3 contenedores
```
En el navegador:
- Front: `http://IP_PUBLICA:5173/`
- Back:  `http://IP_PUBLICA:8089/people`

---

## 8. Comandos de Docker para el día a día

```bash
docker ps                    # contenedores corriendo
docker ps -a                 # TODOS (incluidos apagados)
docker images                # imágenes que tienes
docker logs -f pmng-back     # ver logs en vivo (Ctrl+C para salir, el contenedor sigue)
docker restart pmng-back     # reiniciar
docker stop pmng-back        # detener
docker rm pmng-back          # borrar (debe estar detenido)
docker rmi backend:v1.0      # borrar una imagen
docker network ls            # ver redes
docker exec -it pmng-mysql bash   # entrar DENTRO de un contenedor
```

### Volver a desplegar un cambio en el back (ciclo típico)
```bash
docker stop pmng-back && docker rm pmng-back
cd /home/ec2-user/people-service
docker build -f DockerFile -t backend:v1.0 .
docker run -d --name pmng-back --network pmng-net \
  -e DB_URL="jdbc:mysql://pmng-mysql:3306/people-dev?createDatabaseIfNotExist=true&serverTimezone=UTC" \
  -e DB_USER=root -e DB_PASS=root -p 8089:8080 backend:v1.0
```

---

## 9. Problemas comunes (y qué revisar)

| Síntoma | Causa típica | Solución |
|---------|--------------|----------|
| El back no conecta a MySQL | MySQL aún no arranca, o no están en la misma red | Espera 30s; verifica `--network pmng-net` en ambos; usa `pmng-mysql` no `localhost` |
| El front carga pero no trae datos | La URL en `app.js` apunta a localhost | Cámbiala a `IP_PUBLICA:8089` |
| No entro desde el navegador | Puerto cerrado en el Security Group | Abre 8089 y 5173 en AWS |
| "Cannot connect to the Docker daemon" | Docker no está iniciado / faltan permisos | `sudo service docker start`; cierra SSH y reconecta tras `usermod` |
| Error CORS | Falta `@CrossOrigin` en el controller | Ya lo tienes con `@CrossOrigin(origins="*")` |
| `port is already allocated` | Ya hay algo usando ese puerto externo | Cambia el puerto externo o para el contenedor que lo usa |

---

## 10. Resumen mental (para memorizar)

1. **Dockerfile** = receta → `docker build` → **imagen** → `docker run` → **contenedor**.
2. Puertos: **`-p EXTERNO:INTERNO`**. El interno vive en el Dockerfile/app; el externo lo decides al `run`.
3. El **mismo Dockerfile de Spring Boot sirve para cualquier CRUD** — no cambias nada, solo el puerto externo en `docker run`.
4. **Red (`pmng-net`)**: los contenedores se hablan por **nombre** (`pmng-mysql`), no por localhost ni IP.
5. **Desde afuera** (navegador) → **IP pública + puerto publicado**. **Entre contenedores** → **nombre + puerto interno**.
6. Subir a EC2: `scp` con la llave `.pem` → `unzip` → `docker build` → `docker run`.
7. Dos "firewalls" que deben abrir el puerto: el **Security Group de AWS** y el **`-p` de Docker**.

> Cuando domines la guía 01 (CRUD de memoria) y esta, el siguiente paso natural es
> **docker-compose** (un solo archivo `.yml` que levanta los 3 contenedores con un comando),
> pero eso lo dejamos para después.
