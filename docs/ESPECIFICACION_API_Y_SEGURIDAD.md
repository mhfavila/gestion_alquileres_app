# Especificación de API REST y Modelo de Seguridad

> **Proyecto:** Sistema de Gestión de Alquileres Multi-Contexto (`gestion_alquileres_app`)
> **Base URL:** `/api` (Proxy local `http://localhost:4200/api` → `http://localhost:8080/api`)

---

## 1. Modelo de Seguridad Perimetral y Cabeceras Obligatorias

Todas las peticiones dirigidas a `/api/**` (excepto preflights CORS `OPTIONS`) son interceptadas por `ContextFilter` (`@Order(Ordered.HIGHEST_PRECEDENCE)`) y deben incluir las siguientes cabeceras HTTP:

| Cabecera HTTP | Obligatoria en | Valores Permitidos | Descripción |
| :--- | :--- | :--- | :--- |
| **`X-App-Pin`** | Todas las rutas `/api/**` | Clave maestra activa | Validada contra `configuracion_seguridad` (`APP_PASSWORD`). Si falta o es incorrecta devuelve `401 Unauthorized` (o `429 Too Many Requests` al 5º fallo por IP). |
| **`X-Context-Id`** | `/api/finanzas/**` y `/api/inmobiliaria/**` | `REAL_ESTATE` \| `HOME` \| `PERSONAL` | Fija el contexto en `ContextoHolder` (`ThreadLocal`) aislando todas las lecturas y escrituras JPA. Si falta o es inválido devuelve `400 Bad Request`. |

### 1.1 Defensa en Profundidad en `SeguridadService` y `ContextFilter`
1. **Extracción de IP Real tras Proxy:** Evalúa en orden `X-Forwarded-For` (primer tramo antes de `,`), `X-Real-IP` y `request.getRemoteAddr()`.
2. **Rate-Limiting Anti-Fuerza Bruta (Orden Estricto):** Antes de invocar `SeguridadService`, comprueba si la IP está bloqueada en `ConcurrentHashMap`. Al acumular **5 fallos seguidos**, bloquea la IP durante **15 minutos** devolviendo `429 Too Many Requests`. Un login válido reinicia el contador.
3. **Caché Rápida SHA-256 (`<1 ms`):** Mantiene en memoria durante `60 s` el par `(SHA256(pin), hashBCryptEnBD)` para que las ráfagas de 6 peticiones simultáneas de Angular al cambiar de pestaña no penalicen la CPU recalculando `BCrypt.matches()`.
4. **Invalidación Reactiva y Auto-BCrypt desde SQL:** El valor leído de PostgreSQL tiene un TTL de `10 s` y, si una clave falla contra la caché en memoria, `SeguridadService` fuerza una relectura inmediata de PostgreSQL antes de rechazarla. Si detecta un valor en texto plano en `configuracion_seguridad`, lo valida y lo reemplaza automáticamente por su hash BCrypt (`$2a$10$...`).

---

## 2. Catálogo Completo de Endpoints REST

### 2.1 Seguridad (`/api/seguridad`)

#### `PUT /api/seguridad/password`
Actualiza la contraseña maestra validando primero la contraseña actual y cifrando la nueva con BCrypt.
- **Request Body:**
  ```json
  {
    "passwordActual": "1234",
    "passwordNueva": "MiNuevaClave2026"
  }
  ```
- **Respuestas:**
  - `204 No Content`: Contraseña actualizada y hasheada en BCrypt.
  - `400 Bad Request`: La nueva contraseña tiene menos de 4 caracteres.
  - `401 Unauthorized`: `passwordActual` o `X-App-Pin` incorrectos.

---

### 2.2 Módulo Finanzas (`/api/finanzas`)

#### Wallets (`/api/finanzas/wallets`)
| Método | Ruta | Descripción | Respuesta Exitosa |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/finanzas/wallets` | Devuelve las wallets del contexto activo con `saldoActual` y `saldoRealCaja` calculados en **una única consulta SQL agregada** (`LEFT JOIN transacciones GROUP BY w.id`). | `200 OK` (`List<Wallet>`) |
| `POST` | `/api/finanzas/wallets` | Crea una wallet en el contexto activo. | `200 OK` (`Wallet`) |
| `DELETE` | `/api/finanzas/wallets/{id}` | Elimina una wallet del contexto si no tiene transacciones (`409` si tiene movimientos). | `204 No Content` |

- **Ejemplo Respuesta `GET /api/finanzas/wallets`:**
  ```json
  [
    {
      "id": 1,
      "nombre": "Cartera Inmobiliaria",
      "contexto": "REAL_ESTATE",
      "saldoActual": 550.00,
      "saldoRealCaja": 550.00
    }
  ]
  ```

#### Categorías (`/api/finanzas/categorias`)
| Método | Ruta | Descripción | Respuesta Exitosa |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/finanzas/categorias` | Lista las categorías del contexto ordenadas alfabéticamente. | `200 OK` (`List<Categoria>`) |
| `POST` | `/api/finanzas/categorias` | Crea una categoría en el contexto activo (`nombre` único por contexto). | `200 OK` (`Categoria`) |
| `PUT` | `/api/finanzas/categorias/{id}` | Renombra una categoría existente del mismo contexto (`409` si duplica nombre). | `200 OK` (`Categoria`) |
| `DELETE` | `/api/finanzas/categorias/{id}` | Elimina una categoría sin transacciones (`409 Conflict` si tiene transacciones asociadas). | `204 No Content` |

#### Transacciones y Traspasos (`/api/finanzas/transacciones`)
| Método | Ruta y Query Params | Descripción | Respuesta Exitosa |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/finanzas/transacciones?mes=YYYY-MM&anio=YYYY` | Lista las transacciones del contexto ordenadas por `fecha DESC`. Si se envía `mes` (`YYYY-MM`) o `anio` (`YYYY`), filtra en PostgreSQL mediante `BETWEEN inicio AND fin`. | `200 OK` (`List<Transaccion>`) |
| `POST` | `/api/finanzas/transacciones` | Registra un `INGRESO` o `GASTO` validando que `wallet`, `categoria`, `inmueble` (opc.) y `contrato` (opc.) pertenezcan al contexto activo. | `200 OK` (`Transaccion`) |
| `PUT` | `/api/finanzas/transacciones/{id}` | Actualiza una transacción. Si tiene `traspasoParId != null`, ejecuta **edición en espejo** sincronizando importe, fecha y descripción con su gemela del otro contexto. | `200 OK` (`Transaccion`) |
| `DELETE` | `/api/finanzas/transacciones/{id}` | Elimina una transacción. Si tiene `traspasoParId != null`, ejecuta **borrado en espejo** eliminando también la transacción gemela del otro contexto en la misma transacción ACID. | `204 No Content` |
| `POST` | `/api/finanzas/transacciones/traspaso` | Crea atómicamente un traspaso entre contextos generando un `GASTO` en la wallet origen y un `INGRESO` en la wallet principal del contexto destino bajo la categoría `"Traspaso Interno"`, enlazados por `traspaso_par_id`. | `204 No Content` |

- **Ejemplo Payload `POST /api/finanzas/transacciones`:**
  ```json
  {
    "importe": 850.00,
    "tipo": "INGRESO",
    "fecha": "2026-11-18",
    "descripcion": "Alquiler mensual (2026-11) - Laura Gómez (Piso Castellana 10)",
    "wallet": { "id": 1 },
    "categoria": { "id": 1 },
    "inmueble": { "id": 1 },
    "contrato": { "id": 1 },
    "ocrProcesado": false
  }
  ```

- **Ejemplo Payload `POST /api/finanzas/transacciones/traspaso`:**
  ```json
  {
    "walletOrigenId": 1,
    "contextoDestino": "HOME",
    "importe": 450.00,
    "fecha": "2026-11-18",
    "descripcion": "Aportación mensual hogar"
  }
  ```

---

### 2.3 Módulo Inmobiliaria (`/api/inmobiliaria`)

#### Inmuebles (`/api/inmobiliaria/inmuebles`)
| Método | Ruta | Descripción | Respuesta Exitosa |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/inmobiliaria/inmuebles` | Lista los inmuebles del contexto ordenados por dirección. | `200 OK` (`List<Inmueble>`) |
| `POST` | `/api/inmobiliaria/inmuebles` | Crea un inmueble (`PISO`, `CHALET`, `LOCAL`, `GARAJE`). | `200 OK` (`Inmueble`) |
| `PUT` | `/api/inmobiliaria/inmuebles/{id}` | Actualiza tipo, dirección, referencia y metros de un inmueble. | `200 OK` (`Inmueble`) |
| `DELETE` | `/api/inmobiliaria/inmuebles/{id}` | Elimina un inmueble (`409 Conflict` si tiene contratos o transacciones vinculados). | `204 No Content` |

#### Inquilinos (`/api/inmobiliaria/inquilinos`)
| Método | Ruta | Descripción | Respuesta Exitosa |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/inmobiliaria/inquilinos` | Lista los inquilinos del contexto ordenados por nombre. | `200 OK` (`List<Inquilino>`) |
| `POST` | `/api/inmobiliaria/inquilinos` | Crea un inquilino (`409 Conflict` si `documento` ya existe en el contexto). | `200 OK` (`Inquilino`) |
| `PUT` | `/api/inmobiliaria/inquilinos/{id}` | Actualiza datos del inquilino (`409 Conflict` si duplica `documento`). | `200 OK` (`Inquilino`) |
| `DELETE` | `/api/inmobiliaria/inquilinos/{id}` | Elimina un inquilino (`409 Conflict` si tiene contratos asociados). | `204 No Content` |

#### Contratos (`/api/inmobiliaria/contratos`)
| Método | Ruta | Descripción | Respuesta Exitosa |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/inmobiliaria/contratos` | Lista los contratos del contexto ordenados por `fechaInicio DESC`. | `200 OK` (`List<Contrato>`) |
| `POST` | `/api/inmobiliaria/contratos` | Crea un contrato validando `inmuebleId > 0`, `inquilinoId > 0`, `fechaFin >= fechaInicio` (`400`) y no duplicidad `(inmueble, inquilino, fechaInicio)` (`409`). | `200 OK` (`Contrato`) |
| `PUT` | `/api/inmobiliaria/contratos/{id}` | Actualiza un contrato existente validando pertenencia al contexto y fechas. | `200 OK` (`Contrato`) |
| `DELETE` | `/api/inmobiliaria/contratos/{id}` | Elimina un contrato (`409 Conflict` si tiene transacciones asociadas). | `204 No Content` |

---

## 3. Tabla Maestra de Códigos de Estado HTTP

| Código HTTP | Significado | Causas en el Sistema |
| :---: | :--- | :--- |
| **`200 OK`** | Operación completada con cuerpo JSON | `GET`, `POST` de creación de entidad y `PUT` de actualización. |
| **`204 No Content`** | Operación completada sin cuerpo | `DELETE /{id}`, `POST /traspaso` y `PUT /api/seguridad/password`. |
| **`400 Bad Request`** | Error de validación o cabecera de contexto | Falta `X-Context-Id`, contexto inválido, `inmuebleId <= 0`, `fechaFin < fechaInicio`, formato de `?mes=` inválido o cruce de contextos. |
| **`401 Unauthorized`** | Credencial inválida o ausente | Cabecera `X-App-Pin` ausente o no coincidente con `APP_PASSWORD` en `configuracion_seguridad`. |
| **`404 Not Found`** | Recurso inexistente o de otro contexto | El `id` solicitado no existe o pertenece a un contexto distinto al de `X-Context-Id`. |
| **`409 Conflict`** | Violación de unicidad o integridad referencial | Intento de borrar una entidad con registros hijos vinculados (`FK RESTRICT`), DNI de inquilino duplicado, categoría duplicada o contrato duplicado. |
| **`429 Too Many Requests`** | Bloqueo temporal por fuerza bruta | La IP del cliente ha superado 5 intentos fallidos consecutivos de `X-App-Pin` (bloqueo activo durante 15 minutos). |
