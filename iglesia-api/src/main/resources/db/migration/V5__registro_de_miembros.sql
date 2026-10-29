-- Registro de miembros (SPEC 06): configuración de la iglesia, niveles, miembros y grupos.
-- Convención de V1: iglesia_id NOT NULL, UNIQUE (iglesia_id, id) como destino de FK compuestas que impiden
-- referenciar filas de otra iglesia, índices que empiezan por iglesia_id y RLS en cada tabla.

-- Cómo llama cada iglesia a sus grupos. Si no hay fila, la API devuelve «Grupo» / «Grupos».
CREATE TABLE configuracion_iglesia (
    iglesia_id            BIGINT PRIMARY KEY REFERENCES iglesia (id),
    nombre_grupo_singular VARCHAR(40) NOT NULL,
    nombre_grupo_plural   VARCHAR(40) NOT NULL
);

CREATE TABLE nivel (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id BIGINT       NOT NULL REFERENCES iglesia (id),
    nombre     VARCHAR(60)  NOT NULL,
    orden      INTEGER      NOT NULL,
    activo     BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE (iglesia_id, id)
);
CREATE UNIQUE INDEX ux_nivel_nombre ON nivel (iglesia_id, lower(nombre));

CREATE TABLE miembro (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id               BIGINT       NOT NULL REFERENCES iglesia (id),
    nombres                  VARCHAR(100) NOT NULL,
    apellidos                VARCHAR(100) NOT NULL,
    genero                   VARCHAR(10)  CHECK (genero IN ('MASCULINO', 'FEMENINO')),
    fecha_nacimiento         DATE,
    anio_nacimiento_estimado SMALLINT,
    fecha_ingreso            DATE,
    condicion                VARCHAR(12)  CHECK (condicion IN ('ANTIGUO', 'NUEVO', 'RECUPERADO')),
    estado_civil             VARCHAR(12)  CHECK (estado_civil IN ('SOLTERO', 'CASADO', 'DIVORCIADO', 'VIUDO')),
    telefono                 VARCHAR(20),
    correo                   VARCHAR(254),
    direccion                VARCHAR(200),
    distrito_codigo          CHAR(6)      REFERENCES ubigeo_distrito (codigo),
    ocupacion                VARCHAR(100),
    profesion                VARCHAR(100),
    nivel_estudios           VARCHAR(15)  CHECK (nivel_estudios IN ('SIN_ESTUDIOS', 'PRIMARIA', 'SECUNDARIA', 'TECNICO', 'UNIVERSITARIO', 'POSGRADO')),
    centro_estudios          VARCHAR(150),
    peticion                 VARCHAR(1000),
    nivel_id                 BIGINT,
    lider_id                 BIGINT,
    grupo_id                 BIGINT,
    invitado_por_id          BIGINT,
    registrado_por_id        BIGINT,
    es_pastor_principal      BOOLEAN      NOT NULL DEFAULT FALSE,
    activo                   BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (iglesia_id, id),
    FOREIGN KEY (iglesia_id, nivel_id)          REFERENCES nivel   (iglesia_id, id),
    FOREIGN KEY (iglesia_id, lider_id)          REFERENCES miembro (iglesia_id, id),
    FOREIGN KEY (iglesia_id, invitado_por_id)   REFERENCES miembro (iglesia_id, id),
    FOREIGN KEY (iglesia_id, registrado_por_id) REFERENCES miembro (iglesia_id, id),
    CHECK (lider_id IS NULL OR lider_id <> id),
    CHECK (NOT (es_pastor_principal AND lider_id IS NOT NULL)),
    CHECK (fecha_nacimiento IS NULL OR anio_nacimiento_estimado IS NULL)
);
CREATE UNIQUE INDEX ux_miembro_pastor_principal ON miembro (iglesia_id) WHERE es_pastor_principal;
CREATE INDEX ix_miembro_lider  ON miembro (iglesia_id, lider_id);
CREATE INDEX ix_miembro_grupo  ON miembro (iglesia_id, grupo_id);
CREATE INDEX ix_miembro_nombre ON miembro (iglesia_id, lower(apellidos), lower(nombres));

CREATE TABLE grupo (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id      BIGINT       NOT NULL REFERENCES iglesia (id),
    nombre          VARCHAR(100) NOT NULL,
    lider_id        BIGINT,
    anfitrion_id    BIGINT,
    dia_reunion     VARCHAR(10)  CHECK (dia_reunion IN ('LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO')),
    hora_reunion    TIME,
    direccion       VARCHAR(200),
    distrito_codigo CHAR(6)      REFERENCES ubigeo_distrito (codigo),
    descripcion     VARCHAR(500),
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE (iglesia_id, id),
    FOREIGN KEY (iglesia_id, lider_id)     REFERENCES miembro (iglesia_id, id),
    FOREIGN KEY (iglesia_id, anfitrion_id) REFERENCES miembro (iglesia_id, id)
);
CREATE UNIQUE INDEX ux_grupo_nombre ON grupo (iglesia_id, lower(nombre));

-- miembro y grupo se referencian mutuamente: la FK del grupo del miembro se agrega al final.
ALTER TABLE miembro ADD FOREIGN KEY (iglesia_id, grupo_id) REFERENCES grupo (iglesia_id, id);

SELECT habilitar_aislamiento_por_iglesia('configuracion_iglesia');
SELECT habilitar_aislamiento_por_iglesia('nivel');
SELECT habilitar_aislamiento_por_iglesia('miembro');
SELECT habilitar_aislamiento_por_iglesia('grupo');
