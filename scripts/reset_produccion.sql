-- =============================================================================
-- Script de Limpieza y Puesta en Producción (scripts/reset_produccion.sql)
-- Propósito: Vaciar todos los datos de prueba reiniciando secuencias y dejar
-- únicamente las 3 Wallets oficiales y el catálogo base de Categorías reales.
-- =============================================================================

BEGIN;

-- 1. Vaciar todas las tablas operativas y reiniciar contadores de IDs (secuencias)
TRUNCATE TABLE
    transacciones,
    contratos,
    inquilinos,
    inmuebles,
    categorias,
    wallets
RESTART IDENTITY CASCADE;

-- 2. Crear las 3 Wallets Principales Oficiales (1 por contexto)
INSERT INTO wallets (id, nombre, contexto) VALUES
    (1, 'Cartera Inmobiliaria', 'REAL_ESTATE'),
    (2, 'Casa',                 'HOME'),
    (3, 'Cuenta Principal',     'PERSONAL');

-- 3. Catálogo limpio de Categorías reales por defecto para cada contexto
-- Contexto REAL_ESTATE (Cartera Inmobiliaria)
INSERT INTO categorias (nombre, contexto) VALUES
    ('Alquiler Mensual', 'REAL_ESTATE'),
    ('IBI',              'REAL_ESTATE'),
    ('Comunidad',        'REAL_ESTATE'),
    ('Seguro',           'REAL_ESTATE'),
    ('Reparaciones',     'REAL_ESTATE'),
    ('Suministros',      'REAL_ESTATE'),
    ('Traspaso Interno', 'REAL_ESTATE');

-- Contexto HOME (Economía Doméstica)
INSERT INTO categorias (nombre, contexto) VALUES
    ('Supermercado',     'HOME'),
    ('Suministros',      'HOME'),
    ('Seguro',           'HOME'),
    ('Reparaciones',     'HOME'),
    ('Ocio',             'HOME'),
    ('Traspaso Interno', 'HOME');

-- Contexto PERSONAL (Finanzas Personales)
INSERT INTO categorias (nombre, contexto) VALUES
    ('Nómina / Ingresos', 'PERSONAL'),
    ('Supermercado',      'PERSONAL'),
    ('Ocio',              'PERSONAL'),
    ('Suministros',       'PERSONAL'),
    ('Traspaso Interno',  'PERSONAL');

-- 4. Ajustar las secuencias al máximo ID actual
SELECT setval(pg_get_serial_sequence('wallets', 'id'), COALESCE(MAX(id), 1), true) FROM wallets;
SELECT setval(pg_get_serial_sequence('categorias', 'id'), COALESCE(MAX(id), 1), true) FROM categorias;
SELECT setval(pg_get_serial_sequence('inmuebles', 'id'), 1, false);
SELECT setval(pg_get_serial_sequence('inquilinos', 'id'), 1, false);
SELECT setval(pg_get_serial_sequence('contratos', 'id'), 1, false);
SELECT setval(pg_get_serial_sequence('transacciones', 'id'), 1, false);

COMMIT;
