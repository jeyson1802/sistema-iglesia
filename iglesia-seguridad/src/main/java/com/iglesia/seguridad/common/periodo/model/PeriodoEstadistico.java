package com.iglesia.seguridad.common.periodo.model;

import io.swagger.v3.oas.annotations.media.Schema;

/** Periodos del selector de las páginas de inicio (los mismos atajos que el filtro de aportes). */
@Schema(description = "Periodo de las estadísticas: `HOY`, `SEMANA` (desde el lunes), `MES` (desde el 1 del mes), "
		+ "`MES_ANTERIOR` (el mes pasado completo), `ANIO` (desde el 1 de enero) o `PERSONALIZADO` (con `desde` y "
		+ "`hasta`).")
public enum PeriodoEstadistico {

	/** Solo hoy. */
	HOY,

	/** Desde el lunes de esta semana hasta hoy. */
	SEMANA,

	/** Desde el 1 del mes actual hasta hoy. */
	MES,

	/** El mes pasado, del 1 a su último día. */
	MES_ANTERIOR,

	/** Desde el 1 de enero hasta hoy. */
	ANIO,

	/** El rango que se indique en {@code desde} y {@code hasta}. */
	PERSONALIZADO

}
