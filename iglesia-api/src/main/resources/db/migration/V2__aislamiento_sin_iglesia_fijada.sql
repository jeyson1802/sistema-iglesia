-- En una conexión reutilizada del pool, tras una transacción que fijó app.iglesia_id, current_setting
-- devuelve '' (no NULL) y el cast a bigint falla. NULLIF hace que "sin iglesia" signifique "sin filas".
CREATE OR REPLACE FUNCTION habilitar_aislamiento_por_iglesia(tabla regclass) RETURNS void
    LANGUAGE plpgsql AS
$$
BEGIN
    EXECUTE format('ALTER TABLE %s ENABLE ROW LEVEL SECURITY', tabla);
    EXECUTE format(
        'CREATE POLICY aislamiento_iglesia ON %s USING (iglesia_id = NULLIF(current_setting(''app.iglesia_id'', true), '''')::bigint)',
        tabla);
END
$$;

REVOKE ALL ON FUNCTION habilitar_aislamiento_por_iglesia(regclass) FROM PUBLIC;
