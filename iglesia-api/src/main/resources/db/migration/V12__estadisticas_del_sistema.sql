-- SPEC 11: estadísticas del administrador del sistema entre iglesias.
--
-- En la vista global no hay app.iglesia_id y la RLS no deja leer ninguna fila de negocio. En vez de desactivarla o
-- de usar un rol con BYPASSRLS, estas funciones SECURITY DEFINER (dueño: el rol propietario de las tablas, al que la
-- RLS no se aplica) devuelven SOLO agregados: conteos por mes, por condición o por iglesia. Nunca nombres, correos,
-- teléfonos ni identificadores de miembros, y nunca aportes. El rol de la aplicación solo puede ejecutarlas.

-- Cifras del periodo y del periodo anterior (fechas inclusivas).
CREATE FUNCTION estadisticas_sistema_resumen(desde DATE, hasta DATE, anterior_desde DATE, anterior_hasta DATE)
    RETURNS TABLE (
        iglesias                        BIGINT,
        iglesias_nuevas                 BIGINT,
        iglesias_nuevas_anterior        BIGINT,
        iglesias_con_actividad          BIGINT,
        iglesias_con_actividad_anterior BIGINT,
        miembros                        BIGINT,
        miembros_altas                  BIGINT,
        miembros_altas_anterior         BIGINT,
        miembros_bajas                  BIGINT,
        miembros_bajas_anterior         BIGINT
    )
    LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS
$$
    WITH actividad AS (
        SELECT iglesia_id, CAST(creado_en AS date) AS fecha FROM miembro
        UNION ALL
        SELECT iglesia_id, CAST(registrada_en AS date) FROM asistencia
        UNION ALL
        SELECT iglesia_id, CAST(registrado_en AS date) FROM contacto_consolidacion
    )
    SELECT
        (SELECT count(*) FROM iglesia WHERE activa),
        (SELECT count(*) FROM iglesia WHERE CAST(creada_en AS date) BETWEEN desde AND hasta),
        (SELECT count(*) FROM iglesia WHERE CAST(creada_en AS date) BETWEEN anterior_desde AND anterior_hasta),
        (SELECT count(DISTINCT iglesia_id) FROM actividad WHERE fecha BETWEEN desde AND hasta),
        (SELECT count(DISTINCT iglesia_id) FROM actividad WHERE fecha BETWEEN anterior_desde AND anterior_hasta),
        (SELECT count(*) FROM miembro WHERE activo),
        (SELECT count(*) FROM miembro WHERE COALESCE(fecha_ingreso, CAST(creado_en AS date)) BETWEEN desde AND hasta),
        (SELECT count(*) FROM miembro
         WHERE COALESCE(fecha_ingreso, CAST(creado_en AS date)) BETWEEN anterior_desde AND anterior_hasta),
        (SELECT count(*) FROM miembro WHERE NOT activo AND CAST(actualizado_en AS date) BETWEEN desde AND hasta),
        (SELECT count(*) FROM miembro
         WHERE NOT activo AND CAST(actualizado_en AS date) BETWEEN anterior_desde AND anterior_hasta)
$$;

-- Serie por semana o mes (unidad: 'week' o 'month' de date_trunc): crecimiento y actividad registrada por módulo.
CREATE FUNCTION estadisticas_sistema_serie(desde DATE, hasta DATE, unidad TEXT)
    RETURNS TABLE (
        inicio                 DATE,
        iglesias_nuevas        BIGINT,
        miembros_altas         BIGINT,
        miembros_bajas         BIGINT,
        miembros_registrados   BIGINT,
        asistencias_marcadas   BIGINT,
        contactos_consolidacion BIGINT
    )
    LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS
$$
    WITH eventos AS (
        SELECT CAST(creada_en AS date) AS fecha, 'IGLESIA' AS tipo FROM iglesia
        UNION ALL
        SELECT COALESCE(fecha_ingreso, CAST(creado_en AS date)), 'ALTA' FROM miembro
        UNION ALL
        SELECT CAST(actualizado_en AS date), 'BAJA' FROM miembro WHERE NOT activo
        UNION ALL
        SELECT CAST(creado_en AS date), 'REGISTRO' FROM miembro
        UNION ALL
        SELECT CAST(registrada_en AS date), 'ASISTENCIA' FROM asistencia
        UNION ALL
        SELECT CAST(registrado_en AS date), 'CONTACTO' FROM contacto_consolidacion
    )
    SELECT CAST(date_trunc(unidad, fecha) AS date),
           count(*) FILTER (WHERE tipo = 'IGLESIA'),
           count(*) FILTER (WHERE tipo = 'ALTA'),
           count(*) FILTER (WHERE tipo = 'BAJA'),
           count(*) FILTER (WHERE tipo = 'REGISTRO'),
           count(*) FILTER (WHERE tipo = 'ASISTENCIA'),
           count(*) FILTER (WHERE tipo = 'CONTACTO')
    FROM eventos
    WHERE fecha BETWEEN desde AND hasta
    GROUP BY 1
    ORDER BY 1
$$;

-- Miembros activos de todas las iglesias por condición (NULL: sin dato).
CREATE FUNCTION estadisticas_sistema_condiciones()
    RETURNS TABLE (condicion VARCHAR, total BIGINT)
    LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS
$$
    SELECT condicion, count(*) FROM miembro WHERE activo GROUP BY condicion
$$;

-- Iglesias activas por rango de miembros activos (rango: código ordenable del tramo).
CREATE FUNCTION estadisticas_sistema_tamanos()
    RETURNS TABLE (rango VARCHAR, total BIGINT)
    LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS
$$
    WITH tamanos AS (
        SELECT i.id, count(m.id) AS miembros
        FROM iglesia i LEFT JOIN miembro m ON m.iglesia_id = i.id AND m.activo
        WHERE i.activa
        GROUP BY i.id
    )
    SELECT CAST(CASE
               WHEN miembros = 0 THEN 'SIN_MIEMBROS'
               WHEN miembros <= 50 THEN 'HASTA_50'
               WHEN miembros <= 200 THEN 'HASTA_200'
               WHEN miembros <= 500 THEN 'HASTA_500'
               WHEN miembros <= 1000 THEN 'HASTA_1000'
               ELSE 'MAS_DE_1000'
           END AS VARCHAR), count(*)
    FROM tamanos
    GROUP BY 1
$$;

-- Las iglesias activas con más miembros activos: solo el nombre de la iglesia (catálogo de tenants) y el conteo.
CREATE FUNCTION estadisticas_sistema_mayores(limite INTEGER)
    RETURNS TABLE (iglesia_id BIGINT, nombre VARCHAR, miembros BIGINT)
    LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS
$$
    SELECT i.id, i.nombre_comercial, count(m.id)
    FROM iglesia i LEFT JOIN miembro m ON m.iglesia_id = i.id AND m.activo
    WHERE i.activa
    GROUP BY i.id, i.nombre_comercial
    ORDER BY count(m.id) DESC, i.nombre_comercial
    LIMIT limite
$$;

REVOKE ALL ON FUNCTION estadisticas_sistema_resumen(DATE, DATE, DATE, DATE) FROM PUBLIC;
REVOKE ALL ON FUNCTION estadisticas_sistema_serie(DATE, DATE, TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION estadisticas_sistema_condiciones() FROM PUBLIC;
REVOKE ALL ON FUNCTION estadisticas_sistema_tamanos() FROM PUBLIC;
REVOKE ALL ON FUNCTION estadisticas_sistema_mayores(INTEGER) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION estadisticas_sistema_resumen(DATE, DATE, DATE, DATE) TO ${rol_aplicacion};
GRANT EXECUTE ON FUNCTION estadisticas_sistema_serie(DATE, DATE, TEXT) TO ${rol_aplicacion};
GRANT EXECUTE ON FUNCTION estadisticas_sistema_condiciones() TO ${rol_aplicacion};
GRANT EXECUTE ON FUNCTION estadisticas_sistema_tamanos() TO ${rol_aplicacion};
GRANT EXECUTE ON FUNCTION estadisticas_sistema_mayores(INTEGER) TO ${rol_aplicacion};

-- Índices de apoyo de las estadísticas por iglesia (altas por fecha y reuniones por tipo y fecha).
CREATE INDEX ix_miembro_iglesia_ingreso ON miembro (iglesia_id, fecha_ingreso);
CREATE INDEX ix_reunion_iglesia_tipo_fecha ON reunion (iglesia_id, tipo, fecha);
