-- Catálogo fijo de permisos con la forma <modulo>.<accion>. Cada permiso corresponde a código real,
-- por eso solo se agregan por migración y nunca desde la aplicación.
CREATE TABLE permiso (
    codigo      VARCHAR(60)  PRIMARY KEY,
    modulo      VARCHAR(40)  NOT NULL,
    descripcion VARCHAR(200) NOT NULL,
    orden       INT          NOT NULL
);

-- Roles base (iglesia_id NULL, con código) y roles personalizados por iglesia (con rol base).
CREATE TABLE rol (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    iglesia_id     BIGINT,
    codigo         VARCHAR(30),
    nombre         VARCHAR(60)  NOT NULL,
    rol_base_id    BIGINT REFERENCES rol (id),
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ  NOT NULL,
    actualizado_en TIMESTAMPTZ  NOT NULL,
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_rol_codigo UNIQUE (codigo),
    CONSTRAINT ck_rol_base_o_personalizado CHECK (
        (iglesia_id IS NULL AND codigo IS NOT NULL AND rol_base_id IS NULL)
        OR (iglesia_id IS NOT NULL AND codigo IS NULL AND rol_base_id IS NOT NULL))
);

-- El nombre de un rol personalizado es único dentro de su iglesia, sin distinguir mayúsculas.
CREATE UNIQUE INDEX uk_rol_iglesia_nombre ON rol (iglesia_id, lower(nombre)) WHERE iglesia_id IS NOT NULL;
CREATE INDEX ix_rol_iglesia ON rol (iglesia_id);

CREATE TABLE rol_permiso (
    rol_id         BIGINT      NOT NULL REFERENCES rol (id) ON DELETE CASCADE,
    permiso_codigo VARCHAR(60) NOT NULL REFERENCES permiso (codigo),
    PRIMARY KEY (rol_id, permiso_codigo)
);

-- Semilla: catálogo de permisos.
INSERT INTO permiso (codigo, modulo, descripcion, orden) VALUES
    ('miembros.ver',         'Miembros', 'Ver el listado y el detalle de miembros',                    1),
    ('miembros.crear',       'Miembros', 'Registrar miembros',                                         2),
    ('miembros.editar',      'Miembros', 'Modificar datos de miembros',                                3),
    ('miembros.desactivar',  'Miembros', 'Desactivar y reactivar miembros',                            4),
    ('usuarios.ver',         'Usuarios', 'Ver usuarios del sistema',                                   1),
    ('usuarios.administrar', 'Usuarios', 'Crear y editar usuarios y asignarles roles',                 2),
    ('roles.ver',            'Roles',    'Ver roles y sus permisos',                                   1),
    ('roles.administrar',    'Roles',    'Crear, editar, desactivar y reactivar roles personalizados', 2);

-- Semilla: los 4 roles base, globales y de solo lectura.
INSERT INTO rol (codigo, nombre, creado_en, actualizado_en) VALUES
    ('ADMIN_SISTEMA', 'Administrador del sistema', now(), now()),
    ('ADMIN_IGLESIA', 'Administrador de iglesia',  now(), now()),
    ('LIDER',         'Líder',                     now(), now()),
    ('MIEMBRO',       'Miembro',                   now(), now());

-- Semilla: permisos de los roles base. LIDER y MIEMBRO no tienen permisos por ahora.
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, p.codigo
FROM rol r
JOIN permiso p ON p.codigo IN ('usuarios.ver', 'usuarios.administrar', 'roles.ver')
WHERE r.codigo = 'ADMIN_SISTEMA';

INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, p.codigo
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'ADMIN_IGLESIA';

-- usuario_rol: de texto a FK, conservando las asignaciones existentes.
-- Si alguna fila no encontrara su rol base, SET NOT NULL falla y la migración se revierte.
ALTER TABLE usuario_rol ADD COLUMN rol_id BIGINT REFERENCES rol (id);
UPDATE usuario_rol ur SET rol_id = r.id FROM rol r WHERE r.codigo = ur.rol;
ALTER TABLE usuario_rol ALTER COLUMN rol_id SET NOT NULL;
ALTER TABLE usuario_rol DROP CONSTRAINT usuario_rol_pkey;
ALTER TABLE usuario_rol DROP COLUMN rol;   -- elimina también ck_usuario_rol_valido
ALTER TABLE usuario_rol ADD PRIMARY KEY (usuario_id, rol_id);
CREATE INDEX ix_usuario_rol_rol ON usuario_rol (rol_id);
