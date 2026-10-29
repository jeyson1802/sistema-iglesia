-- SPEC 09: asistencia a cultos, eventos y reuniones de grupo, y código del QR personal de cada miembro.

-- Cultos fijos de cada iglesia (nada viene sembrado: cada iglesia crea los suyos).
CREATE TABLE culto_programado (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id BIGINT      NOT NULL REFERENCES iglesia (id),
    nombre     VARCHAR(60) NOT NULL,
    dia_semana VARCHAR(10) NOT NULL CHECK (dia_semana IN ('LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO')),
    hora       TIME        NOT NULL,
    activo     BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE (iglesia_id, id)
);
CREATE UNIQUE INDEX ux_culto_programado_nombre ON culto_programado (iglesia_id, lower(nombre));

-- Cada ocurrencia concreta donde se toma asistencia. Las de cultos y grupos se crean al tomar asistencia ese día.
CREATE TABLE reunion (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id          BIGINT       NOT NULL REFERENCES iglesia (id),
    tipo                VARCHAR(10)  NOT NULL CHECK (tipo IN ('CULTO', 'EVENTO', 'GRUPO')),
    -- Se copia del culto, del grupo o del evento para que el historial no cambie si luego se renombran.
    nombre              VARCHAR(100) NOT NULL,
    fecha               DATE         NOT NULL,
    hora                TIME,
    culto_programado_id BIGINT,
    grupo_id            BIGINT,
    creada_por          BIGINT       NOT NULL,
    creada_en           TIMESTAMPTZ  NOT NULL,
    UNIQUE (iglesia_id, id),
    FOREIGN KEY (iglesia_id, culto_programado_id) REFERENCES culto_programado (iglesia_id, id),
    FOREIGN KEY (iglesia_id, grupo_id)            REFERENCES grupo (iglesia_id, id),
    CONSTRAINT ck_reunion_origen CHECK (
        (tipo = 'CULTO'  AND culto_programado_id IS NOT NULL AND grupo_id IS NULL) OR
        (tipo = 'GRUPO'  AND grupo_id IS NOT NULL AND culto_programado_id IS NULL) OR
        (tipo = 'EVENTO' AND culto_programado_id IS NULL AND grupo_id IS NULL))
);
-- Una sola reunión por culto o por grupo y día: tomar asistencia dos veces el mismo día abre la misma.
CREATE UNIQUE INDEX ux_reunion_culto_fecha ON reunion (iglesia_id, culto_programado_id, fecha) WHERE culto_programado_id IS NOT NULL;
CREATE UNIQUE INDEX ux_reunion_grupo_fecha ON reunion (iglesia_id, grupo_id, fecha) WHERE grupo_id IS NOT NULL;
CREATE INDEX ix_reunion_fecha ON reunion (iglesia_id, fecha DESC);

-- La clave primaria impide marcar dos veces a la misma persona en la misma reunión, aunque marquen dos equipos a la vez.
CREATE TABLE asistencia (
    iglesia_id     BIGINT      NOT NULL REFERENCES iglesia (id),
    reunion_id     BIGINT      NOT NULL,
    miembro_id     BIGINT      NOT NULL,
    medio          VARCHAR(10) NOT NULL CHECK (medio IN ('QR', 'MANUAL')),
    registrada_por BIGINT      NOT NULL,
    registrada_en  TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (iglesia_id, reunion_id, miembro_id),
    FOREIGN KEY (iglesia_id, reunion_id) REFERENCES reunion (iglesia_id, id) ON DELETE CASCADE,
    FOREIGN KEY (iglesia_id, miembro_id) REFERENCES miembro (iglesia_id, id)
);
CREATE INDEX ix_asistencia_miembro ON asistencia (iglesia_id, miembro_id);

COMMENT ON COLUMN reunion.creada_por IS 'usuarioId del token (iglesia-seguridad); sin FK porque vive en otra base';
COMMENT ON COLUMN asistencia.registrada_por IS 'usuarioId del token (iglesia-seguridad); sin FK porque vive en otra base';

-- Código del QR personal. Nulo hasta que se genera (al crear el miembro o al pedir su QR por primera vez).
ALTER TABLE miembro ADD COLUMN codigo_asistencia VARCHAR(16);
CREATE UNIQUE INDEX ux_miembro_codigo_asistencia ON miembro (iglesia_id, codigo_asistencia) WHERE codigo_asistencia IS NOT NULL;

SELECT habilitar_aislamiento_por_iglesia('culto_programado');
SELECT habilitar_aislamiento_por_iglesia('reunion');
SELECT habilitar_aislamiento_por_iglesia('asistencia');
