package com.iglesia.seguridad.common.periodo.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Un punto de una serie: el día, la semana (lunes) o el mes (día 1) y su valor.")
public record PuntoSerieResponse(
		@Schema(description = "Inicio del día, de la semana o del mes.", example = "2026-10-05") LocalDate inicio,
		@Schema(description = "Valor del punto.", example = "42") long valor) {
}
