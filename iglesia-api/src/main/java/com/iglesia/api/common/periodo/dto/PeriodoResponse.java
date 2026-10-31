package com.iglesia.api.common.periodo.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.periodo.model.Granularidad;
import com.iglesia.api.common.periodo.model.PeriodoEstadistico;
import com.iglesia.api.common.periodo.model.RangoPeriodo;

@Schema(description = "Fechas usadas en las estadísticas (inclusivas) y las del periodo anterior con que se comparan.")
public record PeriodoResponse(
		@Schema(description = "Periodo pedido.", example = "MES") PeriodoEstadistico periodo,
		@Schema(description = "Primer día del periodo.", example = "2026-10-01") LocalDate desde,
		@Schema(description = "Último día del periodo (hoy, salvo con MES_ANTERIOR o PERSONALIZADO).", example = "2026-10-30") LocalDate hasta,
		@Schema(description = "Primer día del periodo anterior.", example = "2026-09-01") LocalDate anteriorDesde,
		@Schema(description = "Último día del periodo anterior.", example = "2026-09-30") LocalDate anteriorHasta,
		@Schema(description = "Tamaño de cada punto de las series.", example = "SEMANA") Granularidad granularidad) {

	public static PeriodoResponse de(RangoPeriodo rango) {
		return new PeriodoResponse(rango.periodo(), rango.desde(), rango.hasta(), rango.anteriorDesde(),
				rango.anteriorHasta(), rango.granularidad());
	}

}
