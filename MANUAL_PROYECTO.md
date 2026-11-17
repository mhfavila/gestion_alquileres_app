# Manual del Proyecto - Gestión de Alquileres Multi-Contexto

## 1. Visión General y el Sistema Multi-Contexto

### Los 3 Contextos
La aplicación gestiona tres contextos independientes que nunca se mezclan:

- **REAL_ESTATE (Cartera)**: Gestión inmobiliaria profesional - inmuebles, inquilinos, contratos de alquiler y movimientos financieros asociados.
- **HOME (Casa)**: Economía doméstica compartida - gastos del hogar, facturas, compras del supermercado.
- **PERSONAL (Personal)**: Finanzas personales individuales - salario, ocio, compras personales.

### Viaje Completo de un Clic: Del Botón a PostgreSQL

**1. Usuario pulsa "Cartera" en Angular**
- `context.service.ts`: El método `cambiar('REAL_ESTATE')` actualiza el signal `contextoActual` y guarda en `localStorage` para persistencia.
- `app.component.ts`: El `effect()` detecta el cambio y ejecuta `cargarDatos()` que recarga wallets, categorías, transacciones, inmuebles, inquilinos y contratos.

**2. Petición HTTP al Backend**
- `context.interceptor.ts`: Intercepta todas las peticiones a `/api/finanzas/**` y `/api/inmobiliaria/**`, añadiendo automáticamente el header `X-Context-Id: REAL_ESTATE`.
- `FinanzasService` / `InmobiliariaService`: Envían peticiones GET/POST con el header de contexto.

**3. Filtro de Seguridad en Spring Boot**
- `ContextFilter.java`: Intercepta todas las rutas `/api/**` (antes solo `/api/finanzas`).
- Valida que el header `X-Context-Id` esté presente y sea válido (`REAL_ESTATE`, `HOME` o `PERSONAL`).
- Si falta o es inválido → error 400.
- Si es válido → `ContextoHolder.set(contexto)` almacena el contexto en un `ThreadLocal` para el hilo actual.

**4. Filtrado en Repositorios JPA**
- `WalletService.obtenerPorContexto()`: Llama a `repository.findByContextoOrderByNombreAsc(ContextoHolder.get())`.
- JPA genera SQL: `SELECT * FROM wallets WHERE contexto = 'REAL_ESTATE' ORDER BY nombre ASC`.
- Todas las entidades se filtran automáticamente por el contexto del hilo actual.

**5. Respuesta al Frontend**
- Spring Boot devuelve solo los datos del contexto solicitado.
- Angular actualiza los signals y re-renderiza la UI con los datos filtrados.

---

## 2. Mapa de la Base de Datos (6 Tablas)

### wallets
**Propósito**: Cuentas/billeteras donde se agrupan transacciones financieras.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGSERIAL PK | Identificador único |
| nombre | VARCHAR(120) | Nombre de la wallet (ej: "Cuenta Principal") |
| contexto | VARCHAR(20) | REAL_ESTATE, HOME o PERSONAL |

**Relaciones**: Una wallet tiene muchas transacciones (OneToMany).

### categorias
**Propósito**: Clasificación de transacciones (ej: "Salario", "Supermercado").

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGSERIAL PK | Identificador único |
| nombre | VARCHAR(120) | Nombre de la categoría |
| contexto | VARCHAR(20) | REAL_ESTATE, HOME o PERSONAL |

**Restricción**: Nombre único por contexto (no puede haber "Salario" duplicado en PERSONAL).

**Relaciones**: Una categoría tiene muchas transacciones (OneToMany).

### inmuebles
**Propósito**: Propiedades inmobiliarias gestionadas (solo visible en REAL_ESTATE).

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGSERIAL PK | Identificador único |
| tipo | VARCHAR(20) | CHALET, PISO, LOCAL, GARAJE |
| direccion | VARCHAR(250) | Dirección física |
| referencia | VARCHAR(50) | Referencia interna (ej: "REF-001") |
| metros | INTEGER | Metros cuadrados |
| contexto | VARCHAR(20) | REAL_ESTATE, HOME o PERSONAL |

**Relaciones**: Un inmueble tiene muchos contratos (OneToMany).

### inquilinos
**Propósito**: Personas que alquilan propiedades (solo visible en REAL_ESTATE).

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGSERIAL PK | Identificador único |
| nombre | VARCHAR(150) | Nombre completo |
| email | VARCHAR(150) | Email de contacto |
| telefono | VARCHAR(30) | Teléfono |
| documento | VARCHAR(30) | DNI/NIF/Passport |
| contexto | VARCHAR(20) | REAL_ESTATE, HOME o PERSONAL |

**Restricción**: Documento único por contexto.

**Relaciones**: Un inquilino tiene muchos contratos (OneToMany).

### contratos
**Propósito**: Contratos de alquiler entre inmuebles e inquilinos.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGSERIAL PK | Identificador único |
| inmueble_id | BIGINT FK | Referencia a inmuebles (id, contexto) |
| inquilino_id | BIGINT FK | Referencia a inquilinos (id, contexto) |
| fecha_inicio | DATE | Fecha inicio del contrato |
| fecha_fin | DATE | Fecha fin (NULL = indefinido) |
| importe_mensual | NUMERIC(14,2) | Alquiler mensual (> 0) |
| fianza | NUMERIC(14,2) | Fianza depositada (>= 0) |
| contexto | VARCHAR(20) | REAL_ESTATE, HOME o PERSONAL |

**Relaciones**: 
- ManyToOne → Inmueble
- ManyToOne → Inquilino
- OneToMany → Transacciones (opcional)

### transacciones
**Propósito**: Movimientos financieros (ingresos/gastos).

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGSERIAL PK | Identificador único |
| importe | NUMERIC(14,2) | Importe (> 0) |
| tipo | VARCHAR(10) | INGRESO o GASTO |
| fecha | DATE | Fecha del movimiento |
| descripcion | VARCHAR(500) | Concepto/descripción |
| contexto | VARCHAR(20) | REAL_ESTATE, HOME o PERSONAL |
| wallet_id | BIGINT FK | Referencia a wallets (id, contexto) |
| categoria_id | BIGINT FK | Referencia a categorias (id, contexto) |
| inmueble_id | BIGINT FK | Referencia a inmuebles (id, contexto) - OPCIONAL |
| contrato_id | BIGINT FK | Referencia a contratos (id, contexto) - OPCIONAL |
| url_comprobante | VARCHAR(1000) | URL de factura/comprobante |
| ocr_procesado | BOOLEAN | Si se ha procesado con OCR |

**Relaciones**:
- ManyToOne → Wallet
- ManyToOne → Categoria
- ManyToOne → Inmueble (opcional, solo REAL_ESTATE)
- ManyToOne → Contrato (opcional, solo REAL_ESTATE)

**Clave de diseño**: Las foreign keys son compuestas `(id, contexto)` para garantizar que una transacción PERSONAL nunca pueda referenciar un inmueble REAL_ESTATE.

### Datos de Prueba (data.sql)
Al arrancar la aplicación se cargan estos datos:

**PERSONAL**:
- Wallet: "Cuenta Principal"
- Categorías: "Salario", "Compras"

**HOME**:
- Wallet: "Casa"
- Categorías: "Supermercado", "Luz/Agua"

**REAL_ESTATE**:
- Wallet: "Cartera Inmobiliaria"
- Categorías: "Alquiler Mensual", "Gastos Comunidad/IBI"
- Inmueble: PISO en "Calle Ejemplo 123, Madrid", 85m², REF-001
- Inquilino: "Juan Pérez", email, teléfono 600123456, documento 12345678A

---

## 3. Catálogo de Endpoints del Backend

### Finanzas (/api/finanzas)

| Método | Ruta | Recibe | Devuelve | Validaciones |
|--------|------|--------|----------|---------------|
| GET | /wallets | Header X-Context-Id | List<Wallet> | Requiere contexto válido |
| POST | /wallets | Wallet JSON (sin id) | Wallet creada | nombre obligatorio, contexto se asigna desde header |
| GET | /categorias | Header X-Context-Id | List<Categoria> | Requiere contexto válido |
| POST | /categorias | Categoria JSON (sin id) | Categoria creada | nombre obligatorio, único por contexto |
| GET | /transacciones | Header X-Context-Id | List<Transaccion> | Requiere contexto válido |
| POST | /transacciones | Transaccion JSON (sin id) | Transaccion creada | wallet_id, categoria_id obligatorios; inmueble_id y contrato_id validados si se proporcionan (deben existir y ser del mismo contexto) |

### Inmobiliaria (/api/inmobiliaria)

| Método | Ruta | Recibe | Devuelve | Validaciones |
|--------|------|--------|----------|---------------|
| GET | /inmuebles | Header X-Context-Id | List<Inmueble> | Requiere contexto válido |
| POST | /inmuebles | Inmueble JSON (sin id) | Inmueble creado | tipo, direccion obligatorios; tipo debe ser CHALET/PISO/LOCAL/GARAJE |
| GET | /inquilinos | Header X-Context-Id | List<Inquilino> | Requiere contexto válido |
| POST | /inquilinos | Inquilino JSON (sin id) | Inquilino creado | nombre obligatorio; documento único por contexto |
| GET | /contratos | Header X-Context-Id | List<Contrato> | Requiere contexto válido |
| POST | /contratos | Contrato JSON (sin id) | Contrato creado | inmueble_id, inquilino_id, fecha_inicio, importe_mensual obligatorios; inmueble e inquilino deben existir y ser del mismo contexto; fecha_fin >= fecha_inicio |

### Seguridad
- Todas las rutas requieren header `X-Context-Id` con valor válido.
- `ContextFilter` rechaza peticiones sin header con valor inválido (error 400).
- Preflight OPTIONS se permiten sin header (para CORS).
- CORS configurado para permitir `http://localhost:4200`.

---

## 4. Guía de Uso de la Pantalla Actual (Frontend)

### URL: http://localhost:4200

#### Navbar Superior
- **Selector de contexto**: 3 botones "Cartera", "Casa", "Personal".
- Al cambiar, la pantalla recarga automáticamente todos los datos del nuevo contexto.
- El contexto seleccionado persiste en localStorage.

#### Sección Finanzas (visible en los 3 contextos)

**Tarjetas de Wallets**:
- Muestran nombre, contexto y saldo actual calculado (INGRESOS - GASTOS).
- Saldo en verde si >= 0, rojo si < 0.
- **Formulario "Nueva wallet"**: Input para nombre, botón "Crear wallet".

**Categorías**:
- Lista de categorías como badges/píldoras.
- Botón "Nueva categoría" para mostrar/ocultar formulario.
- **Formulario**: Input para nombre, botón "Crear".

**Formulario "Nueva transacción"**:
- **Wallet**: Select con primera wallet seleccionada por defecto.
- **Categoría**: Select con primera categoría seleccionada por defecto.
- **Tipo**: Select "Gasto" (default) o "Ingreso".
- **Importe**: Input numérico con placeholder "0.00".
- **Fecha**: Input date con fecha de hoy por defecto.
- **Concepto/Descripción**: Input texto opcional.
- **Inmueble (Opcional)**: Solo visible en REAL_ESTATE, select para vincular a inmueble.
- **Botón**: "Añadir transacción".

**Tabla de Transacciones Recientes**:
- Columnas: Fecha, Descripción, Categoría, Importe, Tipo.
- Importe en verde (INGRESO) o rojo (GASTO).
- Badge de tipo con color.
- Si no hay transacciones, mensaje "No hay transacciones".

#### Sección Inmobiliaria (solo visible en REAL_ESTATE)

**Columna Inmuebles**:
- **Formulario**: Select tipo (PISO/CHALET/LOCAL/GARAJE), input dirección, input referencia, input metros, botón "Añadir inmueble".
- **Lista**: Cada inmueble muestra dirección, tipo, metros y referencia si existe.
- Mensaje "No hay inmuebles" si está vacío.

**Columna Inquilinos**:
- **Formulario**: Input nombre, input email, input teléfono, input documento, botón "Añadir inquilino".
- **Lista**: Cada inquilino muestra nombre, email, teléfono y documento.
- Mensaje "No hay inquilinos" si está vacío.

**Columna Contratos**:
- **Formulario**: Select inmueble (por dirección), select inquilino (por nombre), input fecha inicio, input fecha fin, input importe mensual, input fianza, botón "Añadir contrato".
- **Lista**: Cada contrato muestra importe mensual, fianza, fechas, nombre del inmueble e inquilino.
- Mensaje "No hay contratos" si está vacío.

---

## 5. Radiografía Crítica: Qué Falta o Qué No Se Puede Hacer Aún

### Operaciones CRUD Incompletas
- **PUT/DELETE**: No existen endpoints para editar ni eliminar ningún registro (wallets, categorías, transacciones, inmuebles, inquilinos, contratos). Una vez creado, no se puede modificar ni borrar.
- **GET por ID**: No hay endpoints para obtener un registro individual (solo listados completos).

### Funcionalidades Financieras
- **Saldo en Backend**: El saldo de las wallets se calcula en el frontend (JavaScript), no en el backend. No hay endpoint `/api/finanzas/wallets/{id}/saldo`.
- **Filtros por fechas**: No se pueden filtrar transacciones por rango de fechas.
- **Agrupaciones**: No hay endpoints para obtener totales por categoría, por mes, por año.
- **Exportación**: No se pueden exportar transacciones a CSV/PDF.

### Funcionalidades Inmobiliarias
- **Control de impagos**: No hay seguimiento de pagos de alquiler vencidos.
- **Historial de pagos**: No se registran los pagos mensuales de cada contrato.
- **Estado de contratos**: No se indica si un contrato está activo, vencido o próximo a vencer.
- **Documentos**: No hay gestión de documentos (contratos PDF, DNI de inquilinos, etc.).

### Gestión de Comprobantes
- **Subida de archivos**: El campo `url_comprobante` existe en la BD pero no hay endpoint para subir archivos.
- **OCR**: El campo `ocr_procesado` existe pero no hay servicio de OCR implementado.

### Validaciones y Errores
- **Validación de duplicados**: Solo a nivel BD (error 500 genérico), no hay mensajes de error amigables en el frontend.
- **Confirmación de borrado**: No hay diálogos de confirmación (aunque no existe DELETE aún).

### UI/UX
- **Paginación**: Los listados no están paginados (podría ser problema con muchos registros).
- **Búsqueda**: No hay buscador en listados.
- **Ordenamiento**: Solo orden fijo (por nombre, dirección, etc.), no se puede cambiar.
- **Responsive**: La UI es básica con Tailwind pero no está optimizada para móviles.

### Seguridad
- **Autenticación**: No hay sistema de login/usuarios. Cualquiera con acceso al backend puede operar.
- **Autorización**: No hay roles (admin, usuario, etc.).
- **Auditoría**: No hay logs de quién creó/modificó qué registro.

---

## 6. Propuesta de Mejoras y Próximos Pasos

### Prioridad ALTA (Funcionalidad Básica)

1. **Completar CRUD (PUT/DELETE)**
   - Añadir endpoints PUT y DELETE para todas las entidades.
   - Añadir botones de editar/borrar en la UI con confirmación.
   - Validar que no se pueda borrar una wallet con transacciones, un inmueble con contratos, etc.

2. **Sistema de Autenticación**
   - Implementar Spring Security con JWT.
   - Login/Logout en Angular.
   - Proteger todos los endpoints con autenticación.

3. **Cálculo de Saldos en Backend**
   - Endpoint `GET /api/finanzas/wallets/{id}/saldo`.
   - Endpoint `GET /api/finanzas/resumen` con totales por categoría/mes.
   - Mostrar gráficos de evolución de saldo.

4. **Gestión de Pagos de Alquiler**
   - Tabla `pagos` relacionada con `contratos`.
   - Registrar pagos mensuales (fecha, importe, estado).
   - Alerta de impagos (pagos vencidos no pagados).
   - Dashboard de estado de contratos.

### Prioridad MEDIA (Mejoras de Experiencia)

5. **Filtros y Búsqueda**
   - Filtro por rango de fechas en transacciones.
   - Buscador por nombre/descripción.
   - Filtro por categoría en transacciones.

6. **Subida de Comprobantes**
   - Endpoint POST para subir archivos (multipart/form-data).
   - Almacenamiento local o S3.
   - Vista previa de comprobantes en la UI.

7. **Paginación**
   - Implementar Pageable en Spring Boot.
   - Paginación en listados largos (transacciones, inquilinos).

8. **Exportación**
   - Endpoint para exportar transacciones a CSV.
   - Generación de PDF de contratos.

### Prioridad BAJA (Funcionalidades Avanzadas)

9. **OCR de Facturas**
   - Integrar servicio OCR (Tesseract, Google Vision, AWS Textract).
   - Extraer importe, fecha, proveedor automáticamente.
   - Procesamiento asíncrono con cola.

10. **Dashboard Avanzado**
    - Gráficos de ingresos vs gastos por mes.
    - Mapa de ocupación de inmuebles.
    - KPIs: tasa de impagos, rentabilidad por inmueble.

11. **Notificaciones**
    - Alertas de contratos próximos a vencer.
    - Alertas de pagos pendientes.
    - Email automático a inquilinos.

12. **Multi-idioma**
    - Soporte para inglés/español.
    - i18n en Angular.

### Orden Recomendado de Implementación

1. CRUD completo (PUT/DELETE)
2. Autenticación básica
3. Cálculo de saldos en backend
4. Gestión de pagos de alquiler
5. Filtros y búsqueda
6. Subida de comprobantes
7. Paginación
8. Exportación
9. OCR (si se necesita)
10. Dashboard y notificaciones

---

## Stack Tecnológico

### Backend
- **Framework**: Spring Boot 3.5.16
- **Java**: 21
- **ORM**: Spring Data JPA / Hibernate
- **Base de datos**: PostgreSQL
- **Build**: Maven

### Frontend
- **Framework**: Angular 18
- **Lenguaje**: TypeScript
- **Estilos**: Tailwind CSS
- **Build**: Angular CLI
- **HTTP**: Angular HttpClient con interceptores

### Arquitectura
- **Backend**: Arquitectura en capas (Controller → Service → Repository)
- **Frontend**: Standalone components con signals (Angular 17+)
- **Comunicación**: REST API con JSON
- **Contexto**: ThreadLocal en backend, Signal + localStorage en frontend
