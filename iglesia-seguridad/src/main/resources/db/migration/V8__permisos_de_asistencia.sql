-- SPEC 09: permisos del módulo de asistencia (cultos, eventos y reuniones de grupo).
-- asistencia.registrar sirve para crear un perfil «Ujier» que solo marca asistencia.
INSERT INTO permiso (codigo, modulo, descripcion, orden) VALUES
    ('asistencia.registrar',  'Asistencia', 'Tomar asistencia en cultos, eventos y cualquier grupo',  1),
    ('asistencia.grupo',      'Asistencia', 'Tomar y ver la asistencia de los grupos que lidera',     2),
    ('asistencia.ver',        'Asistencia', 'Ver el historial de asistencia de toda la iglesia',      3),
    ('asistencia.configurar', 'Asistencia', 'Administrar los horarios de culto y los eventos',        4);

-- ADMIN_IGLESIA recibe todos salvo asistencia.grupo (asistencia.registrar ya cubre cualquier grupo); LIDER solo
-- asistencia.grupo (iglesia-api lo limita a los grupos que lidera).
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, p.codigo FROM rol r JOIN permiso p ON p.modulo = 'Asistencia' AND p.codigo <> 'asistencia.grupo'
WHERE r.codigo = 'ADMIN_IGLESIA';
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, 'asistencia.grupo' FROM rol r WHERE r.codigo = 'LIDER';
