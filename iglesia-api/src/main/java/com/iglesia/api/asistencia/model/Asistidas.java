package com.iglesia.api.asistencia.model;

/**
 * Ocasiones que le correspondían a alguien en un período y a cuántas asistió.
 *
 * @param esperadas ocasiones que le correspondían (días de culto y reuniones de su grupo)
 * @param asistidas a cuántas de ellas asistió
 */
public record Asistidas(long esperadas, long asistidas) {

	private static final double PORCENTAJE_TOTAL = 100;

	/** Porcentaje de 0 a 100, redondeado; {@code null} si no le correspondía ninguna ocasión («sin datos»). */
	public Integer porcentaje() {
		return esperadas == 0 ? null : (int) Math.round(asistidas * PORCENTAJE_TOTAL / esperadas);
	}

}
