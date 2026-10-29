package com.iglesia.api.asistencia.model;

import com.iglesia.api.asistencia.constant.LimitesSeguimiento;

/**
 * Valores con los que la iglesia decide sus alertas de seguimiento.
 *
 * @param faltasSeguidas ocasiones seguidas sin asistir para la alerta «Faltas seguidas»
 * @param puntosCaida puntos que debe bajar el porcentaje para la alerta «Asistencia en caída»
 * @param diasNuevo días desde el ingreso en que alguien todavía es nuevo
 */
public record ReglasSeguimiento(int faltasSeguidas, int puntosCaida, int diasNuevo) {

	public static ReglasSeguimiento porDefecto() {
		return new ReglasSeguimiento(LimitesSeguimiento.FALTAS_SEGUIDAS_POR_DEFECTO,
				LimitesSeguimiento.PUNTOS_CAIDA_POR_DEFECTO, LimitesSeguimiento.DIAS_NUEVO_POR_DEFECTO);
	}

}
