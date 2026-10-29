-- SPEC 07: permisos del módulo de aportes (diezmos, ofrendas y categorías configurables).
INSERT INTO permiso (codigo, modulo, descripcion, orden) VALUES
    ('aportes.ver',        'Aportes', 'Ver aportes y totales (el líder, solo los suyos)', 1),
    ('aportes.registrar',  'Aportes', 'Registrar aportes',                                2),
    ('aportes.editar',     'Aportes', 'Corregir aportes registrados',                     3),
    ('aportes.anular',     'Aportes', 'Anular aportes con un motivo',                     4),
    ('aportes.configurar', 'Aportes', 'Elegir la moneda y administrar las categorías',    5);

-- ADMIN_IGLESIA recibe todos; LIDER solo aportes.ver (iglesia-api lo limita a sus propios aportes).
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, p.codigo FROM rol r JOIN permiso p ON p.modulo = 'Aportes'
WHERE r.codigo = 'ADMIN_IGLESIA';
INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, 'aportes.ver' FROM rol r WHERE r.codigo = 'LIDER';
