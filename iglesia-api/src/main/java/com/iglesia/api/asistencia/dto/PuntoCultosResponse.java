package com.iglesia.api.asistencia.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cultos de una semana o un mes y asistentes promedio por culto.")
public record PuntoCultosResponse(
		@Schema(description = "Inicio de la semana (lunes) o del mes (día 1).", example = "2026-10-05") LocalDate inicio,
		@Schema(description = "Cultos con reunión registrada.", example = "2") long cultos,
		@Schema(description = "Asistentes promedio por culto; nulo si no hubo cultos.", example = "86.5",
				nullable = true) Double asistentesPromedio) {
}
