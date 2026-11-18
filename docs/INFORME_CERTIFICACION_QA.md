# Informe de Certificación QA y Evidencias E2E — Gestión de Alquileres Multi-Contexto

- **Fecha de ejecución:** `18/11/2026 15:28:24`
- **Entorno:** Angular 18.2 + Spring Boot 3.5.16 (Java 21) + PostgreSQL 17.4
- **Motor de automatización:** Python 3 + Playwright (`chromium`, viewport `1440x900`)
- **Resultado Global:** **20/20 Casos Superados (100% PASS)**
- **Tiempo Total Acumulado de Suite E2E:** `125897 ms` (`125.90 s`)

---

## 1. Matriz de Ejecución de Casos de Prueba (E2E)

| ID | Categoría | Caso de Prueba | Resultado Esperado | Resultado Obtenido | Tiempo | Estado |
| :--- | :--- | :--- | :--- | :--- | :---: | :---: |
| **QA-01** | Seguridad y Perímetro | Pantalla de bloqueo inicial sin PIN en localStorage | Renderiza formulario de bloqueo e impide acceso al dashboard | Pantalla de bloqueo activa (01_seguridad_pantalla_bloqueo.png) | `2265 ms` | ✅ PASS |
| **QA-02** | Seguridad y Perímetro | Rechazo con PIN incorrecto (401 Unauthorized) | Backend devuelve 401 y UI muestra banner 'PIN de acceso inválido o ausente' | HTTP 401 + Banner rojo visible (02_seguridad_pin_incorrecto.png) | `1236 ms` | ✅ PASS |
| **QA-03** | Seguridad y Perímetro | Rate-Limiting Anti-Fuerza Bruta por IP (X-Forwarded-For) | 4 respuestas 401 seguidas de bloqueo 429 Too Many Requests al 5º intento | Secuencia HTTP recibida: [401, 401, 401, 401, 429] | `351 ms` | ✅ PASS |
| **QA-04** | Seguridad y Perímetro | Desbloqueo con clave maestra ('montero') + Auto-BCrypt y Caché SHA-256 | Autentica en <1ms tras hashear en BCrypt y carga el contexto Cartera | Sesión iniciada + Vista Cartera cargada (04_cartera_vista_general_inicial.png) | `3292 ms` | ✅ PASS |
| **QA-05** | Seguridad y Perímetro | Cambio de contraseña en caliente (PUT /api/seguridad/password) y cierre de sesión (🔒 Bloquear) | Cifra nueva clave en BCrypt (204 No Content), limpia localStorage al bloquear y re-autentica | PUT /api/seguridad/password 204 OK + Re-login OK (03_seguridad_modal_cambiar_clave.png) | `4370 ms` | ✅ PASS |
| **QA-06** | Motor Financiero | Categorías en Cartera: Patrón Crear 3 -> Editar 1 (PUT) -> Borrar 1 (DELETE) | Persisten las categorías editadas y se elimina la temporal sin transacciones | Tasas_Basuras y Fondo_Reserva_Act activas (05_cartera_categorias_totales.png) | `6944 ms` | ✅ PASS |
| **QA-07** | Módulo Inmobiliario | Inmuebles: Crear 3 (Piso, Garaje, Local) y Borrar 1 sin contratos (DELETE) | Se crean los 3 activos y se elimina Local Serrano 3 limpiamente | Persisten Piso Castellana 10 y Plaza Garaje Goya 2 | `7127 ms` | ✅ PASS |
| **QA-08** | Módulo Inmobiliario | Validación 409 Conflict por DNI duplicado de Inquilino en el mismo contexto | Backend rechaza con 409 y UI muestra alerta de documento duplicado | HTTP 409 Conflict + Alerta visible en UI | `9518 ms` | ✅ PASS |
| **QA-09** | Módulo Inmobiliario | Edición (PUT) de Inmueble #2 e Inquilino #2 y Borrado (DELETE) de Inquilino #3 | Actualiza dirección/superficie y teléfono, y elimina el 3er inquilino | Garaje editado (18m²) y Carlos Ruiz (Editado) (06_inmobiliaria_alta_edicion_inmuebles_inquilinos.png) | `5360 ms` | ✅ PASS |
| **QA-10** | Módulo Inmobiliario | Validación de formulario de Contratos (bloqueo inquilinoId=0 y fechaFin < fechaInicio) | Impide el envío y muestra banner rojo con el motivo exacto del error | Banner rojo visible en formulario de Contratos (07_contratos_validacion_error_fechas.png) | `2024 ms` | ✅ PASS |
| **QA-11** | Módulo Inmobiliario | Bloqueo 409 Conflict de Contrato duplicado (mismo inmueble + inquilino + fechaInicio) | Backend rechaza con 409 Conflict y limpia/informa en banner de Contratos | HTTP 409 Conflict detectado y mostrado en UI | `10291 ms` | ✅ PASS |
| **QA-12** | Módulo Inmobiliario | Estados dinámicos de Contrato (COBRADO, PENDIENTE, FINALIZADO) y Cobro en 1 clic | Muestra los 3 estados según fechaInicio/fechaFin y genera INGRESO de +850€ al cobrar | 3 estados validados simultáneamente (08_contratos_estados_y_cobro_1clic.png) | `11536 ms` | ✅ PASS |
| **QA-13** | Módulo Inmobiliario | Estado 'AÚN NO INICIADO' en meses previos a fechaInicio, borrado de Contrato #3 y cobro de Contrato #2 | En 2025-12 indica AÚN NO INICIADO; en mes actual cobra +110€ del Garaje | AÚN NO INICIADO verificado + Cobro Garaje (+110€) completado | `10704 ms` | ✅ PASS |
| **QA-14** | Motor Financiero | Transacciones en Cartera (Crear 3 -> Editar 1 -> Borrar 1) y Rentabilidad Neta por Inmueble | Piso Castellana 10: +850€ - 300€ = 550€ Neto | Garaje: +110€ Neto | Wallet: 660€ | Cálculo exacto verificado en vivo (09_finanzas_transacciones_y_rentabilidad_pisos.png) | `12347 ms` | ✅ PASS |
| **QA-15** | Motor Financiero | Traspasos entre contextos: Crear 3, Edición en espejo (400€ -> 450€) y Borrado en espejo (88€) | Saldo Real en Caja refleja salidas (-40.00€) mientras Operativo permanece en 660.00€ | Sincronización en espejo y desacople Operativo OK (10_traspasos_creacion_y_edicion_espejo.png) | `9310 ms` | ✅ PASS |
| **QA-16** | Escalabilidad y Filtro SQL | Filtrado SQL en Servidor (?mes=YYYY-MM / ?anio=YYYY) con Saldo Real en Caja desacoplado | Saldo Real en Caja fijo (-40.00€) en 1 query SQL; Operativo y tabla se recalibran por periodo | Verificado en Mensual (2026-10 / 2026-11) y Anual (11_filtro_temporal_mensual_anual_historico.png) | `3734 ms` | ✅ PASS |
| **QA-17** | Exportación Segura | Descarga de Informe CSV para Excel/Hacienda con BOM UTF-8 y blindaje Anti-CSV Injection | Archivo .csv con BOM \uFEFF, separador ';', celdas entrecomilladas y resumen por inmueble | Archivo 'movimientos_REAL_ESTATE_2026-11.csv' validado byte a byte (12_exportacion_csv_descarga.png) | `519 ms` | ✅ PASS |
| **QA-18** | Aislamiento Multi-Contexto (HOME) | Contexto Casa: Recepción de traspaso espejo (+450€) y CRUD de gastos domésticos (3 -> 1 -> 1) | Saldo Real en Caja = 240.00€ (+450€ traspaso - 210€ gastos) y Operativo = -210.00€ | Cuadre contable exacto en Casa (13_contexto_casa_economia_domestica.png) | `11382 ms` | ✅ PASS |
| **QA-19** | Aislamiento Multi-Contexto (PERSONAL) | Contexto Personal: Recepción de traspaso (+250€) y CRUD de finanzas individuales (3 -> 1 -> 1) | Saldo Real en Caja = 1990.00€ (+250€ traspaso + 1800€ nómina - 60€ ocio) y Operativo = 1740.00€ | Cuadre contable exacto en Personal (14_contexto_personal_finanzas.png) | `11351 ms` | ✅ PASS |
| **QA-20** | Integridad Referencial (409 Conflict) | Intento de borrado de Inmueble con Contratos/Transacciones vinculados | PostgreSQL y Spring Boot bloquean el borrado con 409 Conflict y la UI muestra banner de alerta | HTTP 409 Conflict + Alerta de protección visible (15_integridad_referencial_y_borrados.png) | `2236 ms` | ✅ PASS |

---

## 2. Catálogo de Evidencias Visuales Generadas (`docs/screenshots/`)

| Nº | Archivo de Evidencia | Caso(s) Cubierto(s) | Descripción de la Evidencia |
| :---: | :--- | :---: | :--- |
| 01 | `01_seguridad_pantalla_bloqueo.png` | `QA-01` | Pantalla de bloqueo perimetral solicitando clave maestra (`X-App-Pin`). |
| 02 | `02_seguridad_pin_incorrecto.png` | `QA-02`, `QA-03` | Rechazo `401 Unauthorized` con alerta roja ante clave inválida y rate-limiting `429`. |
| 03 | `03_seguridad_modal_cambiar_clave.png` | `QA-05` | Formulario desplegable `🔑 Cambiar clave` (`PUT /api/seguridad/password`) con cifrado BCrypt. |
| 04 | `04_cartera_vista_general_inicial.png` | `QA-04` | Contexto `Cartera` (`REAL_ESTATE`) recién desbloqueado con su Wallet Principal única. |
| 05 | `05_cartera_categorias_totales.png` | `QA-06` | Píldoras de Categorías tras aplicar el patrón Crear 3 → Editar 1 (`PUT`) → Borrar 1 (`DELETE`). |
| 06 | `06_inmobiliaria_alta_edicion_inmuebles_inquilinos.png` | `QA-07`, `QA-08`, `QA-09` | CRUD de Inmuebles e Inquilinos con formulario de Inmuebles activo en modo edición (`✏️`). |
| 07 | `07_contratos_validacion_error_fechas.png` | `QA-10` | Validación en formulario de Contratos bloqueando `fechaFin < fechaInicio` con banner rojo. |
| 08 | `08_contratos_estados_y_cobro_1clic.png` | `QA-11`, `QA-12`, `QA-13` | Tarjetas de Contratos mostrando `COBRADO ESTE MES`, `PENDIENTE DE COBRO` y `CONTRATO FINALIZADO`. |
| 09 | `09_finanzas_transacciones_y_rentabilidad_pisos.png` | `QA-14` | Tabla de movimientos y cálculo de Rentabilidad Neta (`+Ingresos / -Gastos / Neto`) por inmueble. |
| 10 | `10_traspasos_creacion_y_edicion_espejo.png` | `QA-15` | Edición en espejo de un traspaso entre contextos con aviso ámbar y selectores bloqueados. |
| 11 | `11_filtro_temporal_mensual_anual_historico.png` | `QA-16` | Selector de Periodo Temporal en modo `Anual` manteniendo fijo el Saldo Real en Caja (`-40.00€`). |
| 12 | `12_exportacion_csv_descarga.png` | `QA-17` | Exportación `📥 Exportar CSV` validada con BOM UTF-8 (`\uFEFF`), delimitador `;` y escapado seguro. |
| 13 | `13_contexto_casa_economia_domestica.png` | `QA-18` | Contexto `Casa` (`HOME`) con traspaso espejo sincronizado (`+450.00€`) y gastos domésticos. |
| 14 | `14_contexto_personal_finanzas.png` | `QA-19` | Contexto `Personal` (`PERSONAL`) con traspaso recibido (`+250.00€`), nómina y gastos de ocio. |
| 15 | `15_integridad_referencial_y_borrados.png` | `QA-20` | Protección de integridad referencial (`409 Conflict`) al intentar borrar un inmueble con contratos. |

---

## 3. Verificación Matemática de Saldos y Cuadre Contable Multi-Contexto

Durante la ejecución automatizada se verificó la exactitud contable en las 3 cuentas principales:

1. **`Cartera Inmobiliaria` (`REAL_ESTATE`):**
   - **Ingresos Operativos:** `+850.00 €` (Alquiler Piso Castellana 10) + `+110.00 €` (Alquiler Garaje Goya 2) = **`+960.00 €`**
   - **Gastos Operativos:** `-180.00 €` (IBI) + `-120.00 €` (Reparación fontanería editada) = **`-300.00 €`**
   - **Resultado Operativo del Periodo (sin traspasos):** `960.00 € - 300.00 €` = **`+660.00 €`**
   - **Traspasos Internos Emitidos:** `-450.00 €` (a `HOME`, editado en espejo) + `-250.00 €` (a `PERSONAL`) = **`-700.00 €`**
   - **Saldo Real en Caja (`wallet.saldoActual` en SQL):** `660.00 € - 700.00 €` = **`-40.00 €`** ✅

2. **`Casa` (`HOME`):**
   - **Traspasos Internos Recibidos:** **`+450.00 €`** (sincronizado en espejo desde `REAL_ESTATE`; el traspaso de `88.00 €` fue eliminado en espejo)
   - **Gastos Operativos Domésticos:** `-135.00 €` (Supermercado) + `-75.00 €` (Suministros editado) = **`-210.00 €`**
   - **Resultado Operativo del Periodo:** **`-210.00 €`**
   - **Saldo Real en Caja (`wallet.saldoActual` en SQL):** `450.00 € - 210.00 €` = **`+240.00 €`** ✅

3. **`Cuenta Principal` (`PERSONAL`):**
   - **Traspasos Internos Recibidos:** **`+250.00 €`** (desde `REAL_ESTATE`)
   - **Ingresos Personales:** **`+1800.00 €`** (Nómina mensual)
   - **Gastos Personales:** **`-60.00 €`** (Ocio editado)
   - **Resultado Operativo del Periodo:** `1800.00 € - 60.00 €` = **`+1740.00 €`**
   - **Saldo Real en Caja (`wallet.saldoActual` en SQL):** `250.00 € + 1740.00 €` = **`+1990.00 €`** ✅