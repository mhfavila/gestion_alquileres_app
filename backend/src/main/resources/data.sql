-- Datos semilla para PERSONAL
INSERT INTO wallets (id, nombre, contexto) VALUES (1, 'Cuenta Principal', 'PERSONAL')
ON CONFLICT (id) DO NOTHING;

INSERT INTO categorias (id, nombre, contexto) VALUES (1, 'Salario', 'PERSONAL')
ON CONFLICT (id) DO NOTHING;

INSERT INTO categorias (id, nombre, contexto) VALUES (2, 'Compras', 'PERSONAL')
ON CONFLICT (id) DO NOTHING;

-- Datos semilla para HOME
INSERT INTO wallets (id, nombre, contexto) VALUES (2, 'Casa', 'HOME')
ON CONFLICT (id) DO NOTHING;

INSERT INTO categorias (id, nombre, contexto) VALUES (3, 'Supermercado', 'HOME')
ON CONFLICT (id) DO NOTHING;

INSERT INTO categorias (id, nombre, contexto) VALUES (4, 'Luz/Agua', 'HOME')
ON CONFLICT (id) DO NOTHING;

-- Datos semilla para REAL_ESTATE
INSERT INTO wallets (id, nombre, contexto) VALUES (3, 'Cartera Inmobiliaria', 'REAL_ESTATE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO categorias (id, nombre, contexto) VALUES (5, 'Alquiler Mensual', 'REAL_ESTATE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO categorias (id, nombre, contexto) VALUES (6, 'Gastos Comunidad/IBI', 'REAL_ESTATE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO inmuebles (id, tipo, direccion, referencia, metros, contexto)
VALUES (1, 'PISO', 'Calle Ejemplo 123, Madrid', 'REF-001', 85, 'REAL_ESTATE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO inquilinos (id, nombre, email, telefono, documento, contexto)
VALUES (1, 'Juan Pérez', 'juan.perez@email.com', '600123456', '12345678A', 'REAL_ESTATE')
ON CONFLICT (id) DO NOTHING;

-- Actualizar secuencias para que los IDs explícitos no causen conflictos en futuros inserts
SELECT setval(pg_get_serial_sequence('wallets', 'id'), COALESCE(MAX(id), 1)) FROM wallets;
SELECT setval(pg_get_serial_sequence('categorias', 'id'), COALESCE(MAX(id), 1)) FROM categorias;
SELECT setval(pg_get_serial_sequence('inmuebles', 'id'), COALESCE(MAX(id), 1)) FROM inmuebles;
SELECT setval(pg_get_serial_sequence('inquilinos', 'id'), COALESCE(MAX(id), 1)) FROM inquilinos;
SELECT setval(pg_get_serial_sequence('contratos', 'id'), COALESCE(MAX(id), 1)) FROM contratos;
SELECT setval(pg_get_serial_sequence('transacciones', 'id'), COALESCE(MAX(id), 1)) FROM transacciones;
