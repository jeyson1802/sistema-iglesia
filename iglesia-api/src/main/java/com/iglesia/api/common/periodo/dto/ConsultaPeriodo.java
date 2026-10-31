package com.iglesia.api.common.periodo.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.format.annotation.DateTimeFormat;

import com.iglesia.api.common.periodo.model.PeriodoEstadistico;

/** Parámetros de consulta del periodo de las estadísticas ({@code ?periodo=&desde=&hasta=}). */
public record ConsultaPeriodo(
		@Parameter(description = "Periodo de las cifras: `HOY`, `SEMANA`, `MES` (por defecto), `MES_ANTERIOR`, "
				+ "`ANIO` o `PERSONALIZADO`. Se compara con el periodo anterior de la misma duración.",
				example = "MES") PeriodoEstadistico periodo,
		@Parameter(description = "Primer día del rango (incluido). Obligatorio solo con `PERSONALIZADO`; con los "
				+ "demás periodos se ignora.", example = "2026-09-01")
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
		@Parameter(description = "Último día del rango (incluido), no posterior a hoy y hasta 366 días después de "
				+ "`desde`. Obligatorio solo con `PERSONALIZADO`.", example = "2026-10-15")
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

	public static ConsultaPeriodo de(PeriodoEstadistico periodo) {
		return new ConsultaPeriodo(periodo, null, null);
	}

	/** El periodo pedido, o {@code MES} si no se indicó. */
	public PeriodoEstadistico periodoPedido() {
		return periodo == null ? PeriodoEstadistico.MES : periodo;
	}

}
