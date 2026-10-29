-- Registro de miembros (SPEC 06): permisos de los catálogos de niveles y grupos de iglesia-api.

INSERT INTO permiso (codigo, modulo, descripcion, orden) VALUES
    ('niveles.administrar', 'Niveles', 'Crear, renombrar, ordenar y desactivar niveles', 1),
    ('grupos.ver',          'Grupos',  'Ver los grupos de la iglesia',                    1),
    ('grupos.administrar',  'Grupos',  'Crear, editar y desactivar grupos',               2);

-- ADMIN_IGLESIA recibe los permisos nuevos, así los roles personalizados de una iglesia pueden marcarlos.
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, p.codigo
FROM rol r
JOIN permiso p ON p.codigo IN ('niveles.administrar', 'grupos.ver', 'grupos.administrar')
WHERE r.codigo = 'ADMIN_IGLESIA';

-- LIDER ve el registro de miembros; iglesia-api limita lo que ve a su red y a sus superiores.
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, 'miembros.ver'
FROM rol r
WHERE r.codigo = 'LIDER';
