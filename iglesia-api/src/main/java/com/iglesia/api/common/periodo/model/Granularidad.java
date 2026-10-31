package com.iglesia.api.common.periodo.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tamaño de cada punto de una serie: día, semana (de lunes a domingo) o mes. Coincide con {@code date_trunc} y se
 * elige según la duración del periodo, para que la serie no tenga ni un solo punto ni cientos.
 */
@Schema(description = "Cada punto de la serie es un `DIA`, una `SEMANA` (de lunes a domingo) o un `MES`.")
public enum Granularidad {

	DIA("day"),

	SEMANA("week"),

	MES("month");

	/** Hasta estos días, la serie va por día. */
	static final long DIAS_MAXIMOS_POR_DIA = 14;

	/** Hasta estos días (unas 13 semanas), la serie va por semana; si son más, por mes. */
	static final long DIAS_MAXIMOS_POR_SEMANA = 92;

	private final String unidadSql;

	Granularidad(String unidadSql) {
		this.unidadSql = unidadSql;
	}

	/** Granularidad para un periodo de {@code dias} días. */
	public static Granularidad para(long dias) {
		if (dias <= DIAS_MAXIMOS_POR_DIA) {
			return DIA;
		}
		return dias <= DIAS_MAXIMOS_POR_SEMANA ? SEMANA : MES;
	}

	/** Unidad de {@code date_trunc} de PostgreSQL. */
	public String unidadSql() {
		return unidadSql;
	}

}
