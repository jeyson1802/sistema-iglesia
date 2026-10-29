-- Reglas de las alertas de seguimiento de asistencia de cada iglesia. Sin fila se usan los valores por defecto.
CREATE TABLE configuracion_seguimiento (
    iglesia_id      BIGINT   PRIMARY KEY REFERENCES iglesia (id),
    faltas_seguidas SMALLINT NOT NULL DEFAULT 3  CHECK (faltas_seguidas BETWEEN 2 AND 10),
    puntos_caida    SMALLINT NOT NULL DEFAULT 30 CHECK (puntos_caida BETWEEN 10 AND 80),
    dias_nuevo      SMALLINT NOT NULL DEFAULT 90 CHECK (dias_nuevo BETWEEN 30 AND 180)
);

-- Las ocasiones de seguimiento recorren las reuniones por tipo y fecha (días de culto, reuniones de grupo).
CREATE INDEX ix_reunion_tipo_fecha ON reunion (iglesia_id, tipo, fecha);

SELECT habilitar_aislamiento_por_iglesia('configuracion_seguimiento');
