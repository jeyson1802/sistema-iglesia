-- SPEC 10: consolidación por rondas. Equipo de consolidadores, rondas (lotes), casos (una persona en una ronda o en un
-- seguimiento individual), contactos (cada llamada, mensaje o visita) y el catálogo de resultados de cada iglesia.

CREATE TABLE consolidador (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id BIGINT      NOT NULL REFERENCES iglesia (id),
    miembro_id BIGINT      NOT NULL,
    activo     BOOLEAN     NOT NULL DEFAULT TRUE,
    creado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (iglesia_id, miembro_id),
    FOREIGN KEY (iglesia_id, miembro_id) REFERENCES miembro (iglesia_id, id)
);

CREATE TABLE resultado_consolidacion (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id BIGINT      NOT NULL REFERENCES iglesia (id),
    nombre     VARCHAR(60) NOT NULL,
    orden      SMALLINT    NOT NULL,
    activo     BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE (iglesia_id, id)
);
CREATE UNIQUE INDEX ux_resultado_consolidacion_nombre ON resultado_consolidacion (iglesia_id, lower(nombre));

CREATE TABLE ronda_consolidacion (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id   BIGINT       NOT NULL REFERENCES iglesia (id),
    nombre       VARCHAR(80)  NOT NULL,
    descripcion  VARCHAR(500),
    fecha_inicio DATE         NOT NULL,
    fecha_fin    DATE,
    estado       VARCHAR(12)  NOT NULL DEFAULT 'PLANIFICADA' CHECK (estado IN ('PLANIFICADA', 'EN_CURSO', 'CERRADA')),
    creada_por   BIGINT       NOT NULL,
    creado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    cerrada_en   TIMESTAMPTZ,
    UNIQUE (iglesia_id, id),
    CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);
CREATE UNIQUE INDEX ux_ronda_consolidacion_nombre ON ronda_consolidacion (iglesia_id, lower(nombre));
CREATE INDEX ix_ronda_consolidacion_estado ON ronda_consolidacion (iglesia_id, estado, fecha_inicio DESC);

-- Usuarios (creada_por, creado_por, cerrado_por, registrado_por) viven en iglesia_seguridad: se guarda el usuarioId
-- del token, sin FK.
CREATE TABLE caso_consolidacion (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id         BIGINT        NOT NULL REFERENCES iglesia (id),
    ronda_id           BIGINT,
    miembro_id         BIGINT        NOT NULL,
    consolidador_id    BIGINT,
    estado             VARCHAR(12)   NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'EN_CONTACTO', 'CERRADO')),
    resultado_id       BIGINT,
    observacion_cierre VARCHAR(1000),
    proximo_contacto   DATE,
    cerrado_en         TIMESTAMPTZ,
    cerrado_por        BIGINT,
    creado_por         BIGINT        NOT NULL,
    creado_en          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (iglesia_id, id),
    FOREIGN KEY (iglesia_id, ronda_id)        REFERENCES ronda_consolidacion (iglesia_id, id),
    FOREIGN KEY (iglesia_id, miembro_id)      REFERENCES miembro (iglesia_id, id),
    FOREIGN KEY (iglesia_id, consolidador_id) REFERENCES miembro (iglesia_id, id),
    FOREIGN KEY (iglesia_id, resultado_id)    REFERENCES resultado_consolidacion (iglesia_id, id),
    CHECK ((estado = 'CERRADO') = (cerrado_en IS NOT NULL))
);
-- Una persona no puede tener dos casos abiertos a la vez, ni estar dos veces en la misma ronda.
CREATE UNIQUE INDEX ux_caso_abierto_por_miembro ON caso_consolidacion (iglesia_id, miembro_id) WHERE estado <> 'CERRADO';
CREATE UNIQUE INDEX ux_caso_ronda_miembro ON caso_consolidacion (iglesia_id, ronda_id, miembro_id) WHERE ronda_id IS NOT NULL;
CREATE INDEX ix_caso_consolidador ON caso_consolidacion (iglesia_id, consolidador_id, estado);
CREATE INDEX ix_caso_miembro ON caso_consolidacion (iglesia_id, miembro_id);
CREATE INDEX ix_caso_ronda ON caso_consolidacion (iglesia_id, ronda_id, estado);

CREATE TABLE contacto_consolidacion (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id       BIGINT        NOT NULL REFERENCES iglesia (id),
    caso_id          BIGINT        NOT NULL,
    consolidador_id  BIGINT,
    fecha_hora       TIMESTAMPTZ   NOT NULL,
    medio            VARCHAR(14)   NOT NULL CHECK (medio IN ('LLAMADA', 'WHATSAPP', 'VISITA', 'EN_LA_IGLESIA')),
    resultado        VARCHAR(16)   NOT NULL CHECK (resultado IN ('SE_HABLO', 'NO_RESPONDIO', 'NO_ESTABA', 'DATO_INCORRECTO')),
    observaciones    VARCHAR(1000),
    proximo_contacto DATE,
    registrado_por   BIGINT        NOT NULL,
    registrado_en    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    FOREIGN KEY (iglesia_id, caso_id)         REFERENCES caso_consolidacion (iglesia_id, id),
    FOREIGN KEY (iglesia_id, consolidador_id) REFERENCES miembro (iglesia_id, id)
);
CREATE INDEX ix_contacto_caso ON contacto_consolidacion (iglesia_id, caso_id, fecha_hora DESC);

SELECT habilitar_aislamiento_por_iglesia('consolidador');
SELECT habilitar_aislamiento_por_iglesia('resultado_consolidacion');
SELECT habilitar_aislamiento_por_iglesia('ronda_consolidacion');
SELECT habilitar_aislamiento_por_iglesia('caso_consolidacion');
SELECT habilitar_aislamiento_por_iglesia('contacto_consolidacion');

-- Resultados iniciales para las iglesias que ya existen. Las nuevas los reciben al darse de alta (IglesiaCreada).
INSERT INTO resultado_consolidacion (iglesia_id, nombre, orden)
SELECT i.id, r.nombre, r.orden
FROM iglesia i
CROSS JOIN (VALUES ('Se integró a un grupo', 1), ('Volverá a asistir', 2), ('Necesita otra visita', 3),
                   ('No le interesa por ahora', 4), ('Se congrega en otra iglesia', 5),
                   ('No se pudo ubicar', 6)) AS r (nombre, orden);
