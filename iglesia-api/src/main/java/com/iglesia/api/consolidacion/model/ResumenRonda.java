package com.iglesia.api.consolidacion.model;

/**
 * Conteo de los casos de una ronda por estado.
 *
 * @param noConcluidos cerrados sin resultado (al cerrar la ronda)
 * @param vencidos abiertos con próximo contacto anterior a hoy
 */
public record ResumenRonda(Long rondaId, long personas, long pendientes, long enContacto, long cerrados,
		long noConcluidos, long sinAsignar, long vencidos) {

	public static ResumenRonda vacio(Long rondaId) {
		return new ResumenRonda(rondaId, 0, 0, 0, 0, 0, 0, 0);
	}

}
