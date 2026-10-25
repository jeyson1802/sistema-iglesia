-- Usuarios del sistema. La iglesia y el miembro se referencian por identificador:
-- viven en iglesia-api, que tiene su propia base de datos (no se comparten tablas).
CREATE TABLE usuario (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    correo          VARCHAR(254) NOT NULL,
    hash_contrasena VARCHAR(255) NOT NULL,
    iglesia_id      BIGINT,
    miembro_id      BIGINT,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ  NOT NULL,
    actualizado_en  TIMESTAMPTZ  NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_usuario_correo UNIQUE (correo),
    CONSTRAINT ck_usuario_miembro_requiere_iglesia CHECK (miembro_id IS NULL OR iglesia_id IS NOT NULL)
);

-- Un miembro tiene como máximo un usuario dentro de su iglesia.
CREATE UNIQUE INDEX uk_usuario_iglesia_miembro ON usuario (iglesia_id, miembro_id) WHERE miembro_id IS NOT NULL;
CREATE INDEX ix_usuario_iglesia ON usuario (iglesia_id);

CREATE TABLE usuario_rol (
    usuario_id BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    rol        VARCHAR(30) NOT NULL,
    PRIMARY KEY (usuario_id, rol),
    CONSTRAINT ck_usuario_rol_valido CHECK (rol IN ('ADMIN_SISTEMA', 'ADMIN_IGLESIA', 'LIDER', 'MIEMBRO'))
);

-- Tokens de refresco opacos: solo se guarda su hash SHA-256 (hex), nunca el valor.
CREATE TABLE token_refresco (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    hash_token  CHAR(64)    NOT NULL,
    emitido_en  TIMESTAMPTZ NOT NULL,
    expira_en   TIMESTAMPTZ NOT NULL,
    revocado_en TIMESTAMPTZ,
    CONSTRAINT uk_token_refresco_hash UNIQUE (hash_token)
);

CREATE INDEX ix_token_refresco_usuario_vigente ON token_refresco (usuario_id) WHERE revocado_en IS NULL;
