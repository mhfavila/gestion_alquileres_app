# Arquitectura del Sistema y Diagramas de Flujo de Datos (DDF)

> **Proyecto:** Sistema de Gestión de Alquileres y Finanzas Multi-Contexto (`gestion_alquileres_app`)
> **Stack:** Angular 18.2 (Signals + Standalone) · Spring Boot 3.5.16 (Java 21) · PostgreSQL 17.4 · Docker / Nginx

---

## 1. Visión Arquitectónica y Modelo C4 (Contexto y Contenedores)

El sistema implementa una arquitectura **Multi-Tenant Lógico por Contexto (`REAL_ESTATE`, `HOME`, `PERSONAL`)** sobre una única instancia de base de datos relacional (**Shared Database, Shared Schema** con aislamiento físico garantizado mediante **Claves Foráneas Compuestas `(id, contexto)`**).

### 1.1 Diagrama de Contenedores (C4 Nivel 2)

```mermaid
flowchart TB
    subgraph Cliente["Dispositivo del Usuario (Navegador Web / Móvil)"]
        UI["SPA Angular 18\n(Standalone Components + Signals)"]
        LS[("localStorage\ngestion.contexto | app_pin")]
        UI <--> LS
    end

    subgraph Edge["Capa de Entrada / Reverse Proxy"]
        NGINX["Nginx Alpine / Dev Proxy\n(Puerto 80 / 4200)\nSPA Fallback + Proxy /api/"]
    end

    subgraph Backend["Servidor de Aplicaciones (Spring Boot 3.5 / Java 21)"]
        CF["ContextFilter (OncePerRequestFilter)\n1. Rate-Limiting por IP (429)\n2. Validación X-App-Pin (401)\n3. Resolución X-Context-Id (400)"]
        CH["ContextoHolder\n(ThreadLocal<Contexto>)"]
        SS["SeguridadService\n(Caché SHA-256 60s + Auto-BCrypt)"]
        FIN["Módulo Finanzas\n(WalletService, CategoriaService,\nTransaccionService)"]
        INM["Módulo Inmobiliaria\n(InmuebleService, InquilinoService,\nContratoService)"]

        CF --> SS
        CF --> CH
        CF --> FIN
        CF --> INM
    end

    subgraph Persistencia["Capa de Datos (PostgreSQL 17)"]
        PG[("Base de Datos: gestion_alquileres\n7 Tablas con FKs Compuestas\n(id, contexto)")]
    end

    UI -- "HTTPS / JSON\nHeaders: X-Context-Id, X-App-Pin" --> NGINX
    NGINX -- "Proxy Pass /api/**\nHeaders: X-Forwarded-For, X-Real-IP" --> CF
    SS -- "JDBC (10s TTL + Auto-BCrypt)" --> PG
    FIN -- "Spring Data JPA / Hibernate 6" --> PG
    INM -- "Spring Data JPA / Hibernate 6" --> PG
```

---

## 2. Diagramas de Flujo de Datos (DDF)

### 2.1 DDF Nivel 0 (Diagrama de Contexto)

```mermaid
flowchart LR
    U(["Propietario / Gestor"])
    SYS[["Sistema de Gestión\nMulti-Contexto"]]
    DB[("PostgreSQL 17\nLibro Mayor y Cartera")]
    EXCEL(["Informes CSV / Excel\n(Hacienda / IRPF / Mod. 184)"])

    U -- "1. PIN de acceso + Contexto activo\n2. Altas/Ediciones/Borrados\n3. Filtro Temporal (Mes/Año)" --> SYS
    SYS -- "Saldos en Caja, Resultado Operativo,\nRentabilidad por Piso y Alertas" --> U
    SYS <-->|"Transacciones ACID y\nConsultas Agregadas"| DB
    SYS -- "Descarga UTF-8 con BOM (;)\nBlindada contra CSV Injection" --> EXCEL
```

### 2.2 DDF Nivel 1 (Flujo Detallado de Procesamiento Interno)

```mermaid
flowchart TD
    REQ["Petición HTTP Entrante (/api/**)"] --> OPT{"¿Método OPTIONS?"}
    OPT -- "Sí (Preflight CORS)" --> OK_CORS["200 OK (CorsFilter)"]
    OPT -- "No" --> IP_CHK{"1. ¿IP Bloqueada en\nintentosPorIp (15 min)?"}

    IP_CHK -- "Sí" --> E429["HTTP 429 Too Many Requests"]
    IP_CHK -- "No" --> PIN_CHK{"2. ¿X-App-Pin válido?\n(SeguridadService)"}

    PIN_CHK -- "No" --> INC_FAIL["Incrementa fallos IP\n(Si >= 5 -> Bloquea 15 min)"]
    INC_FAIL --> E401["HTTP 401 Unauthorized / 429"]

    PIN_CHK -- "Sí" --> RST_IP["Resetea contador IP"]
    RST_IP --> RUTA_SEG{"¿Ruta /api/seguridad/**?"}
    RUTA_SEG -- "Sí" --> CTRL_SEG["SeguridadController\n(PUT /password -> BCrypt)"]

    RUTA_SEG -- "No" --> CTX_CHK{"3. ¿X-Context-Id válido?\n(REAL_ESTATE | HOME | PERSONAL)"}
    CTX_CHK -- "No / Ausente" --> E400["HTTP 400 Bad Request"]
    CTX_CHK -- "Sí" --> SET_TL["ContextoHolder.set(contexto)\n(ThreadLocal)"]

    SET_TL --> ROUTER{"Enrutamiento por Dominio"}
    ROUTER -- "/api/finanzas/wallets" --> W_SVC["WalletService\nQuery única agregada LEFT JOIN\n(Sin N+1 -> saldoActual)"]
    ROUTER -- "/api/finanzas/transacciones" --> T_SVC["TransaccionService\nFiltro SQL FechaBetween (mes/año)\n+ Motor de Traspasos en Espejo"]
    ROUTER -- "/api/inmobiliaria/**" --> I_SVC["Inmueble / Inquilino / ContratoService\nValidación de pertenencia al contexto\n+ Prevención duplicados 409"]

    W_SVC --> FINALLY["Bloque finally:\nContextoHolder.clear()"]
    T_SVC --> FINALLY
    I_SVC --> FINALLY
    FINALLY --> RES["Respuesta JSON al Cliente"]
```

---

## 3. Diagramas de Secuencia UML de Flujos Críticos

### 3.1 Autenticación Perimetral, Rate-Limiting, Caché SHA-256 y Auto-BCrypt Híbrido

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuario / Angular
    participant CF as ContextFilter
    participant SS as SeguridadService
    participant DB as PostgreSQL (configuracion_seguridad)

    U->>CF: GET /api/finanzas/wallets (X-App-Pin, X-Context-Id)
    CF->>CF: Extrae IP real (X-Forwarded-For -> X-Real-IP -> remoteAddr)
    alt IP bloqueada (fallos >= 5 en últimos 15 min)
        CF-->>U: 429 Too Many Requests
    else IP permitida
        CF->>SS: validarPin(rawPin)
        SS->>SS: Calcula pinSha256 = SHA-256(rawPin) (<0.05 ms)
        alt Par (pinSha256, valorBd) en caché rápida (TTL 60s)
            SS-->>CF: true (<1 ms, sin ejecutar BCrypt)
        else No está en caché rápida
            SS->>DB: SELECT valor FROM configuracion_seguridad WHERE clave='APP_PASSWORD'
            alt Valor en BD es texto plano (no empieza por $2a$ / $2b$)
                SS->>SS: Compara valorBd.equals(rawPin)
                SS->>SS: nuevoHash = BCrypt.encode(rawPin)
                SS->>DB: UPDATE configuracion_seguridad SET valor = nuevoHash
                SS->>SS: Actualiza caché rápida (60s) y caché BD (10s)
                SS-->>CF: true
            else Valor en BD es Hash BCrypt ($2a$...)
                SS->>SS: BCrypt.matches(rawPin, valorBd)
                SS->>SS: Actualiza caché rápida (60s)
                SS-->>CF: resultado (true / false)
            end
        end
        alt PIN válido
            CF->>CF: resetearIntentosIp(clientIp)
            CF-->>U: 200 OK (Datos del contexto)
        else PIN inválido
            CF->>CF: registrarFalloIp(clientIp)
            CF-->>U: 401 Unauthorized (o 429 si alcanza 5º fallo)
        end
    end
```

### 3.2 Cobro Mensual de Contrato en 1 Clic y Recálculo Reactivo de Saldos

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuario
    participant APP as AppComponent (Signals)
    participant FS as FinanzasService
    participant TC as TransaccionController / Service
    participant WC as WalletController / Service
    participant DB as PostgreSQL

    U->>APP: Pulsa "Cobrar mes (+850 €)" en Contrato #1
    APP->>APP: Determina fechaCobro (hoy si mes actual, o YYYY-MM-01)
    APP->>APP: Resuelve categoría "Alquiler Mensual" y wallet principal
    APP->>FS: POST /api/finanzas/transacciones (INGRESO, contratoId, inmuebleId)
    FS->>TC: HTTP POST (X-Context-Id: REAL_ESTATE, X-App-Pin)
    TC->>DB: Verifica pertenencia de Wallet, Categoría, Inmueble y Contrato a REAL_ESTATE
    TC->>DB: INSERT INTO transacciones (...)
    DB-->>TC: Transacción creada (ID)
    TC-->>APP: 200 OK (Transacción JSON)
    par Refresco Reactivo Paralelo
        APP->>WC: GET /api/finanzas/wallets
        WC->>DB: SELECT new Wallet(..., SUM(ingresos - gastos)) LEFT JOIN transacciones GROUP BY w.id
        DB-->>APP: Wallet con saldoActual actualizado en 1 sola query
    and
        APP->>TC: GET /api/finanzas/transacciones?mes=YYYY-MM
        TC->>DB: SELECT * FROM transacciones WHERE contexto='REAL_ESTATE' AND fecha BETWEEN ...
        DB-->>APP: Lista de transacciones del periodo
    end
    APP->>APP: Recalibra Signals: Badge -> COBRADO ESTE MES, Neto Inmueble y Operativo
```

### 3.3 Creación, Edición (`PUT`) y Borrado (`DELETE`) en Espejo de Traspasos entre Contextos

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuario
    participant APP as AppComponent
    participant TS as TransaccionService (@Transactional)
    participant DB as PostgreSQL

    Note over U,DB: A) Creación Atómica de Traspaso (REAL_ESTATE -> HOME)
    U->>APP: Envía formulario "Traspasar entre cuentas" (400 €, destino HOME)
    APP->>TS: POST /api/finanzas/transacciones/traspaso
    TS->>DB: Obtiene Wallet origen (REAL_ESTATE) y Wallet principal destino (HOME)
    TS->>DB: Resuelve/crea categoría "Traspaso Interno" en ambos contextos
    TS->>DB: INSERT txSalida (GASTO en REAL_ESTATE)
    TS->>DB: INSERT txEntrada (INGRESO en HOME)
    TS->>DB: UPDATE txSalida.traspaso_par_id = txEntrada.id y viceversa
    TS-->>APP: 204 No Content

    Note over U,DB: B) Edición en Espejo (PUT /api/finanzas/transacciones/{id})
    U->>APP: Edita Traspaso (400 € -> 450 €) y pulsa "Guardar cambios"
    APP->>TS: PUT /api/finanzas/transacciones/{id}
    TS->>DB: Detecta existente.traspasoParId != null -> actualizarTraspasoEnEspejo()
    TS->>DB: Carga transacción gemela del otro contexto (findById(traspasoParId))
    TS->>DB: Sincroniza importe (450 €), fecha y descripción en AMBAS filas
    TS-->>APP: 200 OK

    Note over U,DB: C) Borrado en Espejo (DELETE /api/finanzas/transacciones/{id})
    U->>APP: Pulsa 🗑️ sobre un Traspaso y confirma
    APP->>TS: DELETE /api/finanzas/transacciones/{id}
    TS->>DB: Si tx.traspasoParId != null -> DELETE transacción gemela (otro contexto)
    TS->>DB: DELETE transacción actual + flush()
    TS-->>APP: 204 No Content
```
