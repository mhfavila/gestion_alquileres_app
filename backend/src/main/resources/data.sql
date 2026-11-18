-- Configuración de seguridad inicial (PIN / Contraseña por defecto, auto-hasheada a BCrypt al primer uso)
INSERT INTO configuracion_seguridad (clave, valor) VALUES ('APP_PASSWORD', '1234')
ON CONFLICT (clave) DO NOTHING;

-- 3 Wallets Principales Oficiales (1 por contexto)
INSERT INTO wallets (id, nombre, contexto) VALUES
    (1, 'Cartera Inmobiliaria', 'REAL_ESTATE'),
    (2, 'Casa',                 'HOME'),
    (3, 'Cuenta Principal',     'PERSONAL')
ON CONFLICT (id) DO NOTHING;

-- Catálogo limpio de Categorías reales por defecto
INSERT INTO categorias (nombre, contexto) VALUES
    ('Alquiler Mensual',  'REAL_ESTATE'),
    ('IBI',               'REAL_ESTATE'),
    ('Comunidad',         'REAL_ESTATE'),
    ('Seguro',            'REAL_ESTATE'),
    ('Reparaciones',      'REAL_ESTATE'),
    ('Suministros',       'REAL_ESTATE'),
    ('Traspaso Interno',  'REAL_ESTATE'),
    ('Supermercado',      'HOME'),
    ('Suministros',       'HOME'),
    ('Seguro',            'HOME'),
    ('Reparaciones',      'HOME'),
    ('Ocio',              'HOME'),
    ('Traspaso Interno',  'HOME'),
    ('Nómina / Ingresos', 'PERSONAL'),
    ('Supermercado',      'PERSONAL'),
    ('Ocio',              'PERSONAL'),
    ('Suministros',       'PERSONAL'),
    ('Traspaso Interno',  'PERSONAL')
ON CONFLICT (nombre, contexto) DO NOTHING;

-- Ajustar secuencias respetando tablas vacías (is_called = false si MAX(id) es NULL)
SELECT setval(pg_get_serial_sequence('wallets', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM wallets;
SELECT setval(pg_get_serial_sequence('categorias', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM categorias;
SELECT setval(pg_get_serial_sequence('inmuebles', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM inmuebles;
SELECT setval(pg_get_serial_sequence('inquilinos', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM inquilinos;
SELECT setval(pg_get_serial_sequence('contratos', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM contratos;
SELECT setval(pg_get_serial_sequence('transacciones', 'id'), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM transacciones;
