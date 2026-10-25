-- Estructura base multi-iglesia: rol de aplicación, catálogo de iglesias y utilidad de aislamiento por RLS.

-- Rol con el que se conecta la aplicación. Si no existe se crea sin LOGIN; la contraseña y el LOGIN se
-- configuran fuera de las migraciones (scripts/configurar-rol-aplicacion.sql).
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${rol_aplicacion}') THEN
        CREATE ROLE ${rol_aplicacion} NOLOGIN;
    END IF;
END
$$;

GRANT USAGE ON SCHEMA public TO ${rol_aplicacion};
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${rol_aplicacion};
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO ${rol_aplicacion};

-- Catálogo de iglesias (tenants). Dar de alta una iglesia es insertar una fila aquí.
CREATE TABLE iglesia (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre    VARCHAR(150) NOT NULL,
    activa    BOOLEAN      NOT NULL DEFAULT TRUE,
    creada_en TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Habilita Row-Level Security en una tabla de negocio: solo se ven y escriben las filas de la iglesia
-- fijada en la transacción con set_config('app.iglesia_id', ..., true). Sin iglesia fijada no se ve nada.
--
-- Convención para cada tabla de negocio nueva:
--   iglesia_id BIGINT NOT NULL REFERENCES iglesia (id),
--   UNIQUE (iglesia_id, id)            -- destino de FK compuestas que impiden cruzar iglesias
--   índices y unicidades que empiezan por iglesia_id
--   SELECT habilitar_aislamiento_por_iglesia('tabla');
CREATE FUNCTION habilitar_aislamiento_por_iglesia(tabla regclass) RETURNS void
    LANGUAGE plpgsql AS
$$
BEGIN
    EXECUTE format('ALTER TABLE %s ENABLE ROW LEVEL SECURITY', tabla);
    EXECUTE format(
        'CREATE POLICY aislamiento_iglesia ON %s USING (iglesia_id = current_setting(''app.iglesia_id'', true)::bigint)',
        tabla);
END
$$;

REVOKE ALL ON FUNCTION habilitar_aislamiento_por_iglesia(regclass) FROM PUBLIC;
