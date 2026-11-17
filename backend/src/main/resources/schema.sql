-- Libro mayor compartido. El mismo valor de contexto en el apunte
-- referencia a la vez wallet y categoría, así no se pueden mezclar contextos.

CREATE TABLE IF NOT EXISTS wallets (
    id       BIGSERIAL PRIMARY KEY,
    nombre   VARCHAR(120) NOT NULL,
    contexto VARCHAR(20)  NOT NULL CHECK (contexto IN ('REAL_ESTATE', 'HOME', 'PERSONAL')),
    CONSTRAINT uq_wallets_id_contexto UNIQUE (id, contexto)
);

CREATE TABLE IF NOT EXISTS categorias (
    id       BIGSERIAL PRIMARY KEY,
    nombre   VARCHAR(120) NOT NULL,
    contexto VARCHAR(20)  NOT NULL CHECK (contexto IN ('REAL_ESTATE', 'HOME', 'PERSONAL')),
    CONSTRAINT uq_categorias_nombre_contexto UNIQUE (nombre, contexto),
    CONSTRAINT uq_categorias_id_contexto UNIQUE (id, contexto)
);

CREATE TABLE IF NOT EXISTS transacciones (
    id              BIGSERIAL PRIMARY KEY,
    importe         NUMERIC(14, 2) NOT NULL CHECK (importe > 0),
    tipo            VARCHAR(10)    NOT NULL CHECK (tipo IN ('INGRESO', 'GASTO')),
    fecha           DATE           NOT NULL,
    descripcion     VARCHAR(500),
    contexto        VARCHAR(20)    NOT NULL CHECK (contexto IN ('REAL_ESTATE', 'HOME', 'PERSONAL')),
    wallet_id       BIGINT         NOT NULL,
    categoria_id    BIGINT         NOT NULL,
    url_comprobante VARCHAR(1000),
    ocr_procesado   BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_transacciones_wallet
        FOREIGN KEY (wallet_id, contexto) REFERENCES wallets (id, contexto),
    CONSTRAINT fk_transacciones_categoria
        FOREIGN KEY (categoria_id, contexto) REFERENCES categorias (id, contexto)
);

CREATE INDEX IF NOT EXISTS idx_wallets_contexto ON wallets (contexto);
CREATE INDEX IF NOT EXISTS idx_categorias_contexto ON categorias (contexto);
CREATE INDEX IF NOT EXISTS idx_transacciones_wallet_fecha ON transacciones (wallet_id, fecha);
CREATE INDEX IF NOT EXISTS idx_transacciones_categoria ON transacciones (categoria_id);

CREATE TABLE IF NOT EXISTS inmuebles (
    id         BIGSERIAL PRIMARY KEY,
    tipo       VARCHAR(20)  NOT NULL CHECK (tipo IN ('CHALET', 'PISO', 'LOCAL', 'GARAJE')),
    direccion  VARCHAR(250) NOT NULL,
    referencia VARCHAR(50),
    metros     INTEGER,
    contexto   VARCHAR(20)  NOT NULL CHECK (contexto IN ('REAL_ESTATE', 'HOME', 'PERSONAL')),
    CONSTRAINT uq_inmuebles_id_contexto UNIQUE (id, contexto)
);

CREATE TABLE IF NOT EXISTS inquilinos (
    id        BIGSERIAL PRIMARY KEY,
    nombre    VARCHAR(150) NOT NULL,
    email     VARCHAR(150),
    telefono  VARCHAR(30),
    documento VARCHAR(30),
    contexto  VARCHAR(20)  NOT NULL CHECK (contexto IN ('REAL_ESTATE', 'HOME', 'PERSONAL')),
    CONSTRAINT uq_inquilinos_documento_contexto UNIQUE (documento, contexto),
    CONSTRAINT uq_inquilinos_id_contexto UNIQUE (id, contexto)
);

CREATE INDEX IF NOT EXISTS idx_inquilinos_documento
    ON inquilinos (documento)
    WHERE documento IS NOT NULL;

CREATE TABLE IF NOT EXISTS contratos (
    id              BIGSERIAL PRIMARY KEY,
    inmueble_id     BIGINT         NOT NULL,
    inquilino_id    BIGINT         NOT NULL,
    fecha_inicio    DATE           NOT NULL,
    fecha_fin       DATE,
    importe_mensual NUMERIC(14, 2) NOT NULL CHECK (importe_mensual > 0),
    fianza          NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (fianza >= 0),
    contexto        VARCHAR(20)    NOT NULL CHECK (contexto IN ('REAL_ESTATE', 'HOME', 'PERSONAL')),
    CONSTRAINT fk_contratos_inmueble
        FOREIGN KEY (inmueble_id, contexto) REFERENCES inmuebles (id, contexto),
    CONSTRAINT fk_contratos_inquilino
        FOREIGN KEY (inquilino_id, contexto) REFERENCES inquilinos (id, contexto),
    CONSTRAINT ck_contratos_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),
    CONSTRAINT uq_contratos_id_contexto UNIQUE (id, contexto)
);

CREATE INDEX IF NOT EXISTS idx_inmuebles_contexto ON inmuebles (contexto);
CREATE INDEX IF NOT EXISTS idx_inquilinos_contexto ON inquilinos (contexto);
CREATE INDEX IF NOT EXISTS idx_contratos_inmueble ON contratos (inmueble_id);
CREATE INDEX IF NOT EXISTS idx_contratos_inquilino ON contratos (inquilino_id);
CREATE INDEX IF NOT EXISTS idx_contratos_contexto ON contratos (contexto);
