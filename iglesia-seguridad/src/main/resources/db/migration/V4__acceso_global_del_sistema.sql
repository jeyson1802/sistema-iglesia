-- Acceso global del administrador del sistema: catálogo de iglesias y operar dentro de una iglesia elegida.

-- Permisos del catálogo de iglesias. Solo los tiene ADMIN_SISTEMA: como ADMIN_IGLESIA no los tiene, ningún
-- rol personalizado de una iglesia puede marcarlos (sus permisos son un subconjunto de los del rol base).
INSERT INTO permiso (codigo, modulo, descripcion, orden) VALUES
    ('iglesias.ver',         'Iglesias', 'Ver el catálogo de iglesias',                          1),
    ('iglesias.administrar', 'Iglesias', 'Dar de alta iglesias, editarlas y entrar a operarlas', 2);

INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, p.codigo
FROM rol r
JOIN permiso p ON p.codigo IN ('iglesias.ver', 'iglesias.administrar')
WHERE r.codigo = 'ADMIN_SISTEMA';

-- Iglesia en la que opera el administrador del sistema con esta sesión; NULL = vista global. El refresco
-- la conserva al rotar el token. No es FK: el catálogo de iglesias vive en iglesia-api.
ALTER TABLE token_refresco ADD COLUMN iglesia_actuando_id BIGINT;
