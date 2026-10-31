-- El líder ya no ve aportes por defecto (V11): la descripción de aportes.ver se explica por el alcance del rol.
UPDATE permiso
SET descripcion = 'Ver aportes y totales (con alcance «Toda la iglesia», los de todos; si no, solo los suyos)'
WHERE codigo = 'aportes.ver';
