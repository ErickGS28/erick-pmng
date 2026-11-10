# CRUD desde cero (Spring Boot + Front) — lo MÍNIMO para que funcione

> Objetivo: que puedas hacer un CRUD de una entidad **de memoria**.
> Esta guía está basada en tu propio proyecto `practica-3-people-crud` (entidad `People`).
> Olvídate de Docker por ahora: aquí todo corre **en tu PC** contra un MySQL local.

---

## 0. Mapa mental: ¿qué piezas tiene un CRUD?

Un CRUD en Spring Boot son siempre las **mismas 5 capas**. Si memorizas estas 5, puedes repetir cualquier CRUD:

```
Navegador / Postman
        |
        v
[Controller]   <- recibe las peticiones HTTP (GET, POST, PUT, DELETE)
        |
        v
[Service]      <- la lógica (qué hacer con los datos)
        |
        v
[Repository]   <- habla con la base de datos (lo genera Spring solo)
        |
        v
[Entity/Model] <- la tabla, convertida en clase Java
        |
        v
    MySQL
```

- **Entity (Model):** una clase Java = una tabla. Cada atributo = una columna.
- **Repository:** interfaz que te da `save()`, `findAll()`, `findById()`, `deleteById()` GRATIS.
- **Service:** donde pones tu lógica. Llama al repository.
- **Controller:** expone las URLs (`/people`) y dice qué método responde a cada verbo HTTP.
- **ApiResponse (opcional):** un "sobre" para que todas tus respuestas tengan el mismo formato `{ message, data, error, status }`.

CRUD = **C**reate (POST), **R**ead (GET), **U**pdate (PUT), **D**elete (DELETE).

---

## 1. Requisitos en tu PC (una sola vez)

1. **JDK 17** (o 21). Verifica: `java -version`
2. **MySQL** corriendo en local (puerto 3306). Puede ser MySQL Workbench, XAMPP, o el instalador oficial.
3. Un IDE: **IntelliJ IDEA** (recomendado para Spring) o VS Code.
4. **Postman** (o Thunder Client en VS Code) para probar el backend.

> No necesitas crear la base de datos a mano. Con `createDatabaseIfNotExist=true` Spring la crea sola.

---

## 2. Crear el proyecto Spring Boot (Spring Initializr)

Ve a **https://start.spring.io** y elige:

| Campo | Valor |
|-------|-------|
| Project | Maven |
| Language | Java |
| Spring Boot | 3.x (una versión estable) |
| Group | `com.example` |
| Artifact | `people-service` |
| Java | 17 |

**Dependencias (botón ADD DEPENDENCIES):**
- `Spring Web` → para crear los endpoints REST
- `Spring Data JPA` → para hablar con la BD sin escribir SQL
- `MySQL Driver` → el conector a MySQL
- `Lombok` → para ahorrarte getters/setters

Dale **GENERATE**, descarga el `.zip`, descomprímelo y ábrelo en tu IDE.

> En tu proyecto real el `pom.xml` tiene justo estas dependencias:
> `spring-boot-starter-data-jpa`, `spring-boot-starter-web`, `mysql-connector-j`, `lombok`.

---

## 3. Configurar la conexión a MySQL

Abre `src/main/resources/application.properties` y pega esto:

```properties
spring.application.name=people-service

# Conexión a MySQL. En local apunta a localhost.
# El ${VARIABLE:valor_por_defecto} = "usa la variable de entorno si existe,
# si no, usa el valor por defecto". Esto es lo que te permite cambiarlo en Docker sin tocar el código.
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/people-dev?createDatabaseIfNotExist=true&serverTimezone=UTC}
spring.datasource.username=${DB_USER:root}
spring.datasource.password=${DB_PASS:root}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Puerto del backend (8080 por defecto)
server.port=${SERVER_PORT:8080}

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update   # crea/actualiza las tablas solo
spring.jpa.show-sql=true               # muestra el SQL en la consola (útil para aprender)
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

> Cambia `root` / `root` por tu usuario y contraseña de MySQL local si son distintos.

**¿Qué hace `spring.jpa.hibernate.ddl-auto=update`?**
Spring mira tus clases `@Entity` y crea/ajusta las tablas automáticamente. No tienes que escribir `CREATE TABLE`. Para aprender es perfecto. (En producción real se usa con cuidado.)

---

## 4. Estructura de carpetas (paquetes)

Dentro de `src/main/java/com/example/peopleservice/` crea estos paquetes:

```
peopleservice/
 ├─ PeopleServiceApplication.java   (ya viene creado, NO lo toques)
 ├─ model/        -> la entidad y el repository
 ├─ service/      -> la lógica
 ├─ controller/   -> los endpoints
 └─ kernel/       -> clases de apoyo (ApiResponse)
```

> En tu proyecto real el paquete del controller/service se llama `test`, pero lo correcto
> y más claro es llamarlos `controller` y `service`. Aquí usamos los nombres correctos.

---

## 5. La Entidad (el Model) — `model/People.java`

```java
package com.example.peopleservice.model;

import jakarta.persistence.*;
import lombok.Data;

@Data                      // Lombok: crea getters, setters, toString, etc. solo
@Entity                    // Esto es una tabla de la BD
@Table(name = "people")    // La tabla se llamará "people"
public class People {

    @Id                                              // clave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // autoincremental
    private Long id;

    private String nombre;   // una columna "nombre"
}
```

Eso es TODO. Esta clase crea una tabla `people` con columnas `id` y `nombre`.
Para otra entidad, solo cambias el nombre de la clase, la tabla y los atributos.

---

## 6. El Repository — `model/PeopleRepository.java`

```java
package com.example.peopleservice.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PeopleRepository extends JpaRepository<People, Long> {
    // NO escribes nada aquí. Ya tienes gratis:
    // findAll(), findById(id), save(obj), deleteById(id), etc.
}
```

`JpaRepository<People, Long>` significa: "repositorio de la entidad **People**, cuyo id es de tipo **Long**".
Esta sola línea te da todos los métodos de base de datos. **Es la magia de Spring Data JPA.**

---

## 7. (Opcional pero recomendado) El "sobre" de respuesta — `kernel/ApiResponse.java`

Sirve para que todas tus respuestas tengan la misma forma: `{ message, data, error, status }`.
El front espera `apiResponse.data`, así que conviene tenerlo.

```java
package com.example.peopleservice.kernel;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class ApiResponse {
    private String message;
    private Object data;
    private Boolean error;
    private HttpStatus status;

    public ApiResponse(String message, Object data, Boolean error, HttpStatus status) {
        this.message = message;
        this.data = data;
        this.error = error;
        this.status = status;
    }
}
```

---

## 8. El Service — `service/PeopleService.java`

Aquí va la lógica. Este es el **CRUD COMPLETO** (los 4: crear, leer, actualizar, borrar).
Tu proyecto solo tenía GET y POST; aquí lo completamos para que lo aprendas entero.

```java
package com.example.peopleservice.service;

import com.example.peopleservice.kernel.ApiResponse;
import com.example.peopleservice.model.People;
import com.example.peopleservice.model.PeopleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PeopleService {

    @Autowired   // Spring inyecta el repository solo
    private PeopleRepository peopleRepository;

    // READ (todos)
    public ApiResponse getAll() {
        List<People> people = peopleRepository.findAll();
        return new ApiResponse("Personas obtenidas", people, false, HttpStatus.OK);
    }

    // READ (uno por id)
    public ApiResponse getOne(Long id) {
        Optional<People> p = peopleRepository.findById(id);
        if (p.isEmpty()) {
            return new ApiResponse("No existe esa persona", null, true, HttpStatus.NOT_FOUND);
        }
        return new ApiResponse("Persona encontrada", p.get(), false, HttpStatus.OK);
    }

    // CREATE
    public ApiResponse crear(People people) {
        People saved = peopleRepository.save(people);
        return new ApiResponse("Persona creada", saved, false, HttpStatus.CREATED);
    }

    // UPDATE
    public ApiResponse actualizar(Long id, People datos) {
        Optional<People> existente = peopleRepository.findById(id);
        if (existente.isEmpty()) {
            return new ApiResponse("No existe esa persona", null, true, HttpStatus.NOT_FOUND);
        }
        People p = existente.get();
        p.setNombre(datos.getNombre());   // actualizas los campos
        People actualizado = peopleRepository.save(p);
        return new ApiResponse("Persona actualizada", actualizado, false, HttpStatus.OK);
    }

    // DELETE
    public ApiResponse eliminar(Long id) {
        if (!peopleRepository.existsById(id)) {
            return new ApiResponse("No existe esa persona", null, true, HttpStatus.NOT_FOUND);
        }
        peopleRepository.deleteById(id);
        return new ApiResponse("Persona eliminada", null, false, HttpStatus.OK);
    }
}
```

> Fíjate el patrón: **el service siempre llama al repository**. No escribes SQL.

---

## 9. El Controller — `controller/PeopleController.java`

Aquí conectas cada URL + verbo HTTP con un método del service.

```java
package com.example.peopleservice.controller;

import com.example.peopleservice.kernel.ApiResponse;
import com.example.peopleservice.model.People;
import com.example.peopleservice.service.PeopleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController                      // este controlador devuelve JSON
@RequestMapping("/people")           // todas las URLs empiezan con /people
@CrossOrigin(origins = "*")          // permite que el front (otro origen) le llame (evita error CORS)
public class PeopleController {

    @Autowired
    private PeopleService peopleService;

    // GET http://localhost:8080/people
    @GetMapping
    public ResponseEntity<ApiResponse> listar() {
        ApiResponse r = peopleService.getAll();
        return ResponseEntity.status(r.getStatus()).body(r);
    }

    // GET http://localhost:8080/people/1
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> obtener(@PathVariable Long id) {
        ApiResponse r = peopleService.getOne(id);
        return ResponseEntity.status(r.getStatus()).body(r);
    }

    // POST http://localhost:8080/people   body: { "nombre": "Erick" }
    @PostMapping
    public ResponseEntity<ApiResponse> crear(@RequestBody People people) {
        ApiResponse r = peopleService.crear(people);
        return ResponseEntity.status(r.getStatus()).body(r);
    }

    // PUT http://localhost:8080/people/1   body: { "nombre": "Nuevo" }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> actualizar(@PathVariable Long id, @RequestBody People people) {
        ApiResponse r = peopleService.actualizar(id, people);
        return ResponseEntity.status(r.getStatus()).body(r);
    }

    // DELETE http://localhost:8080/people/1
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> eliminar(@PathVariable Long id) {
        ApiResponse r = peopleService.eliminar(id);
        return ResponseEntity.status(r.getStatus()).body(r);
    }
}
```

### Tabla mental de las anotaciones (memorizar esto = memorizar el CRUD)

| Anotación | Para qué sirve |
|-----------|----------------|
| `@RestController` | La clase responde peticiones HTTP con JSON |
| `@RequestMapping("/people")` | Prefijo común de las URLs |
| `@GetMapping` | Responde a GET (leer) |
| `@PostMapping` | Responde a POST (crear) |
| `@PutMapping` | Responde a PUT (actualizar) |
| `@DeleteMapping` | Responde a DELETE (borrar) |
| `@PathVariable` | Lee un valor de la URL (`/people/1` → id=1) |
| `@RequestBody` | Lee el JSON que mandan en el body |
| `@CrossOrigin` | Permite que el front llame al back (CORS) |
| `@Autowired` | Spring te inyecta una dependencia sola |

---

## 10. Arrancar el backend

En tu IDE, ejecuta la clase `PeopleServiceApplication` (botón ▶).
O por terminal, en la carpeta del proyecto:

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

Cuando veas en consola **`Started PeopleServiceApplication`**, ya está corriendo en `http://localhost:8080`.

---

## 11. Probar el CRUD con Postman

| Acción | Método | URL | Body (raw JSON) |
|--------|--------|-----|-----------------|
| Listar | GET | `http://localhost:8080/people` | — |
| Crear | POST | `http://localhost:8080/people` | `{ "nombre": "Erick" }` |
| Obtener uno | GET | `http://localhost:8080/people/1` | — |
| Actualizar | PUT | `http://localhost:8080/people/1` | `{ "nombre": "Nuevo" }` |
| Borrar | DELETE | `http://localhost:8080/people/1` | — |

> En Postman, para POST/PUT: pestaña **Body → raw → JSON**.

Si el POST responde `{ "message": "Persona creada", "data": { "id": 1, "nombre": "Erick" } }`, **tu CRUD funciona**.

---

## 12. El Frontend mínimo

Tu front son 2 archivos: `index.html` y `app.js`. No usa framework, es JS puro con `fetch`.
(Más abajo te dejo también la versión equivalente en React/Next si te la piden.)

### `index.html`

```html
<!doctype html>
<html lang="es">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>People CRUD</title>
  </head>
  <body>
    <h1>Lista de nombres</h1>

    <form>
      <input type="text" placeholder="Ingresa un nombre" />
      <button id="btn" type="submit">Agregar</button>
    </form>

    <ul></ul>

    <script src="app.js"></script>
  </body>
</html>
```

### `app.js`

```js
"use strict";

const d = document;

// IMPORTANTE: en local apunta a localhost:8080.
// Cuando lo subas a la EC2 con Docker, cambiarás esto por http://IP_PUBLICA:8089/people
const url = "http://localhost:8080/people";

const button = d.getElementById("btn");

function agregarALista(persona) {
  const ul = d.querySelector("ul");
  const li = d.createElement("li");
  li.textContent = persona.nombre;
  ul.appendChild(li);
}

// READ: cargar todas al iniciar
function cargarPersonas() {
  fetch(url)
    .then((res) => res.json())
    .then((apiResponse) => {
      const ul = d.querySelector("ul");
      ul.innerHTML = "";
      const personas = apiResponse.data || [];   // <- aquí se usa el "data" del ApiResponse
      personas.forEach(agregarALista);
    })
    .catch((err) => console.error(err));
}

// CREATE: al enviar el formulario
button.addEventListener("click", (e) => {
  e.preventDefault();
  const input = d.querySelector("input");
  if (!input.value.trim()) return;

  button.disabled = true;

  fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ nombre: input.value }),
  })
    .then((res) => res.json())
    .then((apiResponse) => {
      agregarALista(apiResponse.data);
      input.value = "";
    })
    .catch((err) => console.error(err))
    .finally(() => (button.disabled = false));
});

cargarPersonas();
```

### Probar el front en local
Abre `index.html` en el navegador (doble clic) con el backend corriendo.
Como el back tiene `@CrossOrigin(origins = "*")`, el navegador puede llamarlo sin problema.

---

## 13. (Extra) La misma idea en React / Next.js

Si en el futuro te piden el front en **Next.js**, el concepto es idéntico: un `fetch` a la misma API.
Ejemplo de un componente cliente mínimo:

```tsx
"use client";
import { useEffect, useState } from "react";

const API = "http://localhost:8080/people";

export default function People() {
  const [personas, setPersonas] = useState<any[]>([]);
  const [nombre, setNombre] = useState("");

  const cargar = () =>
    fetch(API).then(r => r.json()).then(res => setPersonas(res.data || []));

  useEffect(() => { cargar(); }, []);

  const crear = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!nombre.trim()) return;
    await fetch(API, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ nombre }),
    });
    setNombre("");
    cargar();
  };

  return (
    <div>
      <h1>Lista de nombres</h1>
      <form onSubmit={crear}>
        <input value={nombre} onChange={e => setNombre(e.target.value)} placeholder="Nombre" />
        <button type="submit">Agregar</button>
      </form>
      <ul>{personas.map(p => <li key={p.id}>{p.nombre}</li>)}</ul>
    </div>
  );
}
```

---

## 14. Checklist para hacerlo DE MEMORIA

Practica repetir esto sin mirar. Si logras estos 9 pasos sin copiar, ya dominas el CRUD:

1. Spring Initializr con 4 dependencias: **Web, JPA, MySQL Driver, Lombok**.
2. `application.properties` con la `datasource.url` a MySQL + `ddl-auto=update`.
3. **Entity** con `@Entity`, `@Id`, `@GeneratedValue`.
4. **Repository** = interfaz `extends JpaRepository<Entity, Long>`.
5. (Opcional) **ApiResponse**.
6. **Service** con `@Service` + `@Autowired` del repo, con los 5 métodos (getAll, getOne, crear, actualizar, eliminar).
7. **Controller** con `@RestController` + `@RequestMapping` + los 5 mappings (GET, GET/{id}, POST, PUT/{id}, DELETE/{id}).
8. Arrancar y probar en **Postman**.
9. **Front**: `fetch` a la API, leer `apiResponse.data`.

> Truco para memorizar el orden: **Modelo → Repositorio → Service → Controller**.
> "De abajo hacia arriba": primero la tabla, luego quién la consulta, luego la lógica, al final las URLs.

Cuando esto te salga solo, pasa a `02-DOCKER-Y-EC2-EXPLICADO.md`.
