-- SPEC 08: identidad y apariencia de cada iglesia (denominación, nombre comercial, dirección, contacto, logo, colores
-- y tipografía). La tabla iglesia es el catálogo de tenants: no tiene iglesia_id ni RLS, porque el administrador del
-- sistema la lista y la filtra para todas las iglesias.

-- El nombre actual pasa a ser el nombre comercial: el corto, único y el que se muestra en la topbar.
ALTER TABLE iglesia RENAME COLUMN nombre TO nombre_comercial;
ALTER INDEX ux_iglesia_nombre RENAME TO ux_iglesia_nombre_comercial;

ALTER TABLE iglesia
    ADD COLUMN denominacion        VARCHAR(200),
    ADD COLUMN direccion           VARCHAR(200),
    ADD COLUMN distrito_codigo     CHAR(6) REFERENCES ubigeo_distrito (codigo),
    ADD COLUMN telefono            VARCHAR(20),
    ADD COLUMN correo              VARCHAR(120),
    ADD COLUMN sitio_web           VARCHAR(200),
    ADD COLUMN tema_preset         VARCHAR(10) NOT NULL DEFAULT 'Aura',
    ADD COLUMN color_primario      VARCHAR(20) NOT NULL DEFAULT 'emerald',
    ADD COLUMN color_superficie    VARCHAR(20),
    ADD COLUMN tipografia          VARCHAR(30) NOT NULL DEFAULT 'Playfair Display',
    -- Cuándo se subió el logo vigente (NULL = sin logo). Vive en el catálogo para saber qué iglesias tienen logo y
    -- armar su ETag sin leer la imagen, que está protegida por RLS en logo_iglesia.
    ADD COLUMN logo_actualizado_en TIMESTAMPTZ;

-- Las iglesias existentes conservan su nombre también como denominación hasta que la completen. La dirección y el
-- distrito quedan nulos solo para ellas: la API los exige al crear y al editar.
UPDATE iglesia SET denominacion = nombre_comercial;
ALTER TABLE iglesia ALTER COLUMN denominacion SET NOT NULL;

CREATE INDEX ix_iglesia_distrito ON iglesia (distrito_codigo);

-- El logo va aparte para no cargar la imagen cada vez que se lee la iglesia. Una fila por iglesia.
CREATE TABLE logo_iglesia (
    iglesia_id BIGINT      PRIMARY KEY REFERENCES iglesia (id),
    contenido  BYTEA       NOT NULL,
    tipo       VARCHAR(20) NOT NULL,
    CONSTRAINT ck_logo_iglesia_tipo CHECK (tipo IN ('image/png', 'image/jpeg', 'image/webp'))
);

SELECT habilitar_aislamiento_por_iglesia('logo_iglesia');
