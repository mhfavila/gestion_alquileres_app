create database Gestion_Alquileres;

use Gestion_Alquileres;




-- 1. CREACIÓN DE TIPOS ENUMERADOS
CREATE TYPE tipo_inmueble_enum AS ENUM ('GARAJE', 'VIVIENDA_COMPLETA', 'VIVIENDA_HABITACIONES');
CREATE TYPE lugar_fianza_enum AS ENUM ('CUENTA_PROPIA', 'CAMARA_ALQUILERES');
CREATE TYPE estado_contrato_enum AS ENUM ('ACTIVO', 'FINALIZADO', 'PENDIENTE_FIRMA');
CREATE TYPE tipo_transaccion_enum AS ENUM ('INGRESO', 'GASTO');
CREATE TYPE categoria_transaccion_enum AS ENUM ('ALQUILER', 'COMUNIDAD', 'IBI', 'SEGURO', 'REPARACION', 'SUMINISTROS');
CREATE TYPE estado_pago_enum AS ENUM ('PAGADO', 'PENDIENTE');

-- 2. CREACIÓN DE TABLAS

CREATE TABLE inmueble (
    id SERIAL PRIMARY KEY,
    alias_nombre VARCHAR(100) NOT NULL,
    tipo_inmueble tipo_inmueble_enum NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    numero_habitaciones INT,
    valor_compra NUMERIC(10, 2)
);

CREATE TABLE habitacion (
    id SERIAL PRIMARY KEY,
    inmueble_id INT NOT NULL REFERENCES inmueble(id) ON DELETE CASCADE,
    identificador VARCHAR(100) NOT NULL,
    precio_base NUMERIC(10, 2) NOT NULL,
    suministros_incluidos BOOLEAN DEFAULT FALSE
);

CREATE TABLE inquilino (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(150) NOT NULL,
    documento_identidad VARCHAR(50) UNIQUE NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    email VARCHAR(150),
    estado_laboral VARCHAR(50),
    tipo_contrato VARCHAR(50),
    contacto_emergencia VARCHAR(150),
    fecha_nacimiento DATE,
    nacionalidad VARCHAR(50),
    ciudad_nacimiento VARCHAR(100)
);

CREATE TABLE contrato (
    id SERIAL PRIMARY KEY,
    inquilino_id INT NOT NULL REFERENCES inquilino(id),
    inmueble_id INT NOT NULL REFERENCES inmueble(id),
    habitacion_id INT REFERENCES habitacion(id), -- Null si es garaje o piso completo
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE,
    renta_mensual NUMERIC(10, 2) NOT NULL,
    fianza_depositada BOOLEAN DEFAULT FALSE,
    lugar_fianza lugar_fianza_enum,
    estado estado_contrato_enum DEFAULT 'PENDIENTE_FIRMA',
    ruta_documento_pdf VARCHAR(255)
);

CREATE TABLE transaccion (
    id SERIAL PRIMARY KEY,
    inmueble_id INT NOT NULL REFERENCES inmueble(id),
    contrato_id INT REFERENCES contrato(id), -- Null para gastos generales del piso vacio
    tipo tipo_transaccion_enum NOT NULL,
    categoria categoria_transaccion_enum NOT NULL,
    importe NUMERIC(10, 2) NOT NULL,
    fecha_emision DATE NOT NULL,
    concepto VARCHAR(255) NOT NULL,
    estado_pago estado_pago_enum DEFAULT 'PENDIENTE'
);

-- 3. INSERCIÓN DE DATOS DE PRUEBA (MOCK DATA)

-- Añadimos 3 inmuebles distintos
INSERT INTO inmueble (alias_nombre, tipo_inmueble, direccion, numero_habitaciones, valor_compra) VALUES
('Piso Centro', 'VIVIENDA_COMPLETA', 'Calle Mayor 14, 2B', 3, 150000.00),
('Piso Estudiantes', 'VIVIENDA_HABITACIONES', 'Avenida Universidad 5, 1A', 4, 180000.00),
('Plaza Garaje Centro', 'GARAJE', 'Calle Mayor 14, Sótano 2, Plaza 15', NULL, 15000.00);

-- Añadimos las habitaciones solo para el "Piso Estudiantes" (inmueble_id = 2)
INSERT INTO habitacion (inmueble_id, identificador, precio_base, suministros_incluidos) VALUES
(2, 'Habitación 1 - Exterior Cama Doble', 350.00, TRUE),
(2, 'Habitación 2 - Interior', 250.00, TRUE);

-- Añadimos inquilinos
INSERT INTO inquilino (nombre, apellidos, documento_identidad, telefono, email, estado_laboral) VALUES
('Carlos', 'Ruiz Gómez', '12345678A', '600111222', 'carlos@email.com', 'Indefinido'),
('Laura', 'Martínez', '87654321B', '600333444', 'laura@email.com', 'Estudiante');

-- Creamos los contratos
-- Contrato 1: Carlos alquila el piso completo y el garaje (2 contratos separados o 1, aquí mostramos 1 para el piso)
INSERT INTO contrato (inquilino_id, inmueble_id, habitacion_id, fecha_inicio, fecha_fin, renta_mensual, fianza_depositada, lugar_fianza, estado) VALUES
(1, 1, NULL, '2026-01-01', '2027-01-01', 800.00, TRUE, 'CAMARA_ALQUILERES', 'ACTIVO'),
-- Contrato 2: Laura alquila solo una habitación (Habitación 1 del Piso 2)
(2, 2, 1, '2026-09-01', '2027-06-30', 350.00, TRUE, 'CUENTA_PROPIA', 'ACTIVO');

-- Registramos la actividad financiera (Transacciones)
INSERT INTO transaccion (inmueble_id, contrato_id, tipo, categoria, importe, fecha_emision, concepto, estado_pago) VALUES
-- Ingreso del alquiler de Carlos (Piso Centro)
(1, 1, 'INGRESO', 'ALQUILER', 800.00, '2026-09-01', 'Mensualidad Septiembre', 'PAGADO'),
-- Gasto de comunidad del Piso Centro (No está ligado a un contrato, sino al piso)
(1, NULL, 'GASTO', 'COMUNIDAD', 60.00, '2026-09-05', 'Cuota Comunidad Septiembre', 'PAGADO'),
-- Ingreso del alquiler de Laura (Habitación 1)
(2, 2, 'INGRESO', 'ALQUILER', 350.00, '2026-09-01', 'Mensualidad Septiembre', 'PENDIENTE'),
-- Reparación en el piso de estudiantes
(2, NULL, 'GASTO', 'REPARACION', 120.50, '2026-09-10', 'Arreglo cisterna baño común', 'PAGADO');




--CONSULTAS
--1. Ver quién vive en el "Piso Estudiantes" (Alquiler por habitaciones):
SELECT i.nombre, i.telefono, h.identificador, c.renta_mensual, c.estado 
FROM contrato c
JOIN inquilino i ON c.inquilino_id = i.id
JOIN habitacion h ON c.habitacion_id = h.id
WHERE c.inmueble_id = 2 AND c.estado = 'ACTIVO';

--2. Calcular el beneficio neto mensual del "Piso Centro" (Ingresos - Gastos):
SELECT 
    inmueble.alias_nombre,
    SUM(CASE WHEN tipo = 'INGRESO' AND estado_pago = 'PAGADO' THEN importe ELSE 0 END) as total_ingresado,
    SUM(CASE WHEN tipo = 'GASTO' AND estado_pago = 'PAGADO' THEN importe ELSE 0 END) as total_gastado,
    (SUM(CASE WHEN tipo = 'INGRESO' AND estado_pago = 'PAGADO' THEN importe ELSE 0 END) - 
     SUM(CASE WHEN tipo = 'GASTO' AND estado_pago = 'PAGADO' THEN importe ELSE 0 END)) as beneficio_neto
FROM transaccion
JOIN inmueble ON transaccion.inmueble_id = inmueble.id
WHERE inmueble.id = 1
GROUP BY inmueble.alias_nombre;


--3. Detectar impagos:

SELECT i.nombre, i.telefono, t.concepto, t.importe, t.fecha_emision
FROM transaccion t
JOIN contrato c ON t.contrato_id = c.id
JOIN inquilino i ON c.inquilino_id = i.id
WHERE t.tipo = 'INGRESO' AND t.estado_pago = 'PENDIENTE';
