-- SPEC 08: el administrador de la iglesia personaliza los datos, el logo y la apariencia de su propia iglesia.
-- No es un permiso del catálogo de iglesias (módulo «Iglesias», solo de ADMIN_SISTEMA): se puede repartir en roles
-- personalizados de la iglesia.
INSERT INTO permiso (codigo, modulo, descripcion, orden) VALUES
    ('iglesia.personalizar', 'Mi iglesia', 'Editar los datos, el logo y la apariencia de la iglesia', 1);

INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, 'iglesia.personalizar' FROM rol r WHERE r.codigo = 'ADMIN_IGLESIA';
