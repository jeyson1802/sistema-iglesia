-- SPEC 10: permisos del módulo de consolidación (rondas, casos y contactos).
-- consolidacion.contactar sirve para crear un perfil «Consolidador»; consolidacion.administrar (coordinador) incluye
-- en iglesia-api todo lo de ver y contactar, sin límite de ámbito.
INSERT INTO permiso (codigo, modulo, descripcion, orden) VALUES
    ('consolidacion.administrar', 'Consolidación', 'Coordinar la consolidación: equipo, rondas, reparto y resultados', 1),
    ('consolidacion.ver',         'Consolidación', 'Ver las rondas, su avance y el historial de cualquier persona',   2),
    ('consolidacion.contactar',   'Consolidación', 'Contactar a sus asignados y dar seguimiento a su red',           3);

-- ADMIN_IGLESIA recibe los tres; LIDER, consolidacion.contactar (iglesia-api lo limita a sus casos y a su red).
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, p.codigo FROM rol r JOIN permiso p ON p.modulo = 'Consolidación'
WHERE r.codigo = 'ADMIN_IGLESIA';
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, 'consolidacion.contactar' FROM rol r WHERE r.codigo = 'LIDER';
