package com.iglesia.api.common.estadisticas.constant;

/**
 * Fragmentos SQL comunes de las estadísticas. {@code ALCANCE} define las CTE {@code red} (la red del líder
 * {@code :liderId}, recorrida con una CTE recursiva) y {@code alcance} (los miembros de la iglesia, o solo los de la
 * red si {@code :liderId} no es nulo). Ambas filtran {@code iglesia_id} en cada paso, incluido el recursivo.
 */
public final class ConsultasAlcance {

	public static final String ALCANCE = """
			WITH RECURSIVE red AS (
			    SELECT m.id FROM miembro m
			    WHERE m.iglesia_id = :iglesiaId AND m.lider_id = CAST(:liderId AS bigint)
			    UNION
			    SELECT m.id FROM miembro m JOIN red ON m.lider_id = red.id
			    WHERE m.iglesia_id = :iglesiaId
			),
			alcance AS (
			    SELECT m.* FROM miembro m
			    WHERE m.iglesia_id = :iglesiaId
			      AND (CAST(:liderId AS bigint) IS NULL OR m.id IN (SELECT id FROM red))
			)
			""";

	/** Fecha de alta de un miembro de {@code alcance}: su ingreso o, si no lo tiene, su registro. */
	public static final String FECHA_ALTA = "COALESCE(fecha_ingreso, CAST(creado_en AS date))";

	private ConsultasAlcance() {
	}

}
