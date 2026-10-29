-- SPEC 06, revisión 2: ajustes pedidos al probar el registro de miembros.
-- V5 ya está aplicada en las bases de desarrollo, por eso los cambios van en esta migración.

-- Quita los CHECK de miembro cuya definición menciona la columna indicada. Las restricciones de V5 se declararon sin
-- nombre, así que se buscan por su definición en lugar de depender del nombre que generó PostgreSQL.
CREATE FUNCTION pg_temp.quitar_check_de_miembro(columna TEXT) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    restriccion RECORD;
BEGIN
    FOR restriccion IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'miembro'::regclass AND contype = 'c' AND pg_get_constraintdef(oid) LIKE '%' || columna || '%'
    LOOP
        EXECUTE format('ALTER TABLE miembro DROP CONSTRAINT %I', restriccion.conname);
    END LOOP;
END;
$$;

-- 1. Fecha de nacimiento y edad aproximada son independientes: se pueden guardar las dos.
SELECT pg_temp.quitar_check_de_miembro('anio_nacimiento_estimado');

-- 4. La condición ANTIGUO pasa a llamarse CONSOLIDADO.
SELECT pg_temp.quitar_check_de_miembro('condicion');
UPDATE miembro SET condicion = 'CONSOLIDADO' WHERE condicion = 'ANTIGUO';
ALTER TABLE miembro ADD CONSTRAINT ck_miembro_condicion CHECK (condicion IN ('CONSOLIDADO', 'NUEVO', 'RECUPERADO'));

-- 2. El grupo no tiene nombre: es el grupo de su líder, que pasa a ser obligatorio y único en la iglesia.
--    Solo las bases de desarrollo tienen grupos sin líder o con el líder repetido: se sacan sus miembros (sin borrar
--    personas) y se eliminan, conservando el grupo más antiguo de cada líder.
CREATE TEMPORARY TABLE grupo_descartado ON COMMIT DROP AS
SELECT g.id FROM grupo g
WHERE g.lider_id IS NULL
   OR EXISTS (SELECT 1 FROM grupo o WHERE o.iglesia_id = g.iglesia_id AND o.lider_id = g.lider_id AND o.id < g.id);
UPDATE miembro SET grupo_id = NULL WHERE grupo_id IN (SELECT id FROM grupo_descartado);
DELETE FROM grupo WHERE id IN (SELECT id FROM grupo_descartado);

DROP INDEX ux_grupo_nombre;
ALTER TABLE grupo DROP COLUMN nombre;
ALTER TABLE grupo ALTER COLUMN lider_id SET NOT NULL;
CREATE UNIQUE INDEX ux_grupo_lider ON grupo (iglesia_id, lider_id);

-- 6. Filtro del listado por departamento, provincia o distrito (prefijo del código de distrito).
CREATE INDEX ix_miembro_distrito ON miembro (iglesia_id, distrito_codigo);
