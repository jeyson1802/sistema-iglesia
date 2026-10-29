-- SPEC 07: aportes de los miembros (diezmos, ofrendas y categorías configurables por iglesia).
-- Convención de V1 y V5: iglesia_id NOT NULL, UNIQUE (iglesia_id, id) como destino de FK compuestas que impiden
-- referenciar filas de otra iglesia, índices que empiezan por iglesia_id y Row-Level Security.

-- Moneda de la iglesia para sus aportes. Si no hay fila, la API devuelve PEN.
CREATE TABLE configuracion_aportes (
    iglesia_id BIGINT  PRIMARY KEY REFERENCES iglesia (id),
    moneda     CHAR(3) NOT NULL CHECK (moneda ~ '^[A-Z]{3}$')
);

CREATE TABLE categoria_aporte (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id      BIGINT      NOT NULL REFERENCES iglesia (id),
    nombre          VARCHAR(60) NOT NULL,
    exige_aportante BOOLEAN     NOT NULL DEFAULT FALSE,
    orden           INTEGER     NOT NULL,
    activo          BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE (iglesia_id, id)
);
CREATE UNIQUE INDEX ux_categoria_aporte_nombre ON categoria_aporte (iglesia_id, lower(nombre));

CREATE TABLE aporte (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id       BIGINT        NOT NULL REFERENCES iglesia (id),
    fecha            DATE          NOT NULL,
    miembro_id       BIGINT,
    categoria_id     BIGINT        NOT NULL,
    monto            NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    medio            VARCHAR(15)   NOT NULL CHECK (medio IN ('EFECTIVO', 'TRANSFERENCIA', 'YAPE', 'PLIN', 'CHEQUE', 'OTRO')),
    numero_operacion VARCHAR(40),
    peticion         VARCHAR(1000),
    observacion      VARCHAR(500),
    anulado          BOOLEAN       NOT NULL DEFAULT FALSE,
    motivo_anulacion VARCHAR(300),
    registrado_por   BIGINT        NOT NULL,
    registrado_en    TIMESTAMPTZ   NOT NULL,
    modificado_por   BIGINT,
    modificado_en    TIMESTAMPTZ,
    UNIQUE (iglesia_id, id),
    FOREIGN KEY (iglesia_id, miembro_id)   REFERENCES miembro (iglesia_id, id),
    FOREIGN KEY (iglesia_id, categoria_id) REFERENCES categoria_aporte (iglesia_id, id),
    CONSTRAINT ck_aporte_anulacion CHECK (anulado = (motivo_anulacion IS NOT NULL))
);
COMMENT ON COLUMN aporte.miembro_id IS 'NULL = aporte anónimo';
COMMENT ON COLUMN aporte.registrado_por IS 'usuarioId del token (iglesia-seguridad); sin FK porque vive en otra base';
CREATE INDEX ix_aporte_fecha     ON aporte (iglesia_id, fecha DESC);
CREATE INDEX ix_aporte_miembro   ON aporte (iglesia_id, miembro_id, fecha DESC);
CREATE INDEX ix_aporte_categoria ON aporte (iglesia_id, categoria_id, fecha DESC);

SELECT habilitar_aislamiento_por_iglesia('configuracion_aportes');
SELECT habilitar_aislamiento_por_iglesia('categoria_aporte');
SELECT habilitar_aislamiento_por_iglesia('aporte');

-- Diezmo y Ofrenda para las iglesias que ya existen. Las nuevas los reciben al darse de alta
-- (CategoriasPorDefecto escucha el evento IglesiaCreada).
INSERT INTO categoria_aporte (iglesia_id, nombre, exige_aportante, orden)
SELECT i.id, c.nombre, c.exige_aportante, c.orden
FROM iglesia i
CROSS JOIN (VALUES ('Diezmo', TRUE, 1), ('Ofrenda', FALSE, 2)) AS c (nombre, exige_aportante, orden);
