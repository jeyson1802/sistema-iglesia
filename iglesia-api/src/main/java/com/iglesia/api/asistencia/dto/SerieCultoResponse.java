package com.iglesia.api.asistencia.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Asistentes de un culto programado por semana.")
public record SerieCultoResponse(
		@Schema(description = "Identificador del culto programado.", example = "3") Long cultoProgramadoId,
		@Schema(description = "Nombre actual del culto.", example = "Culto dominical 9:00") String nombre,
		@Schema(description = "Asistentes por semana, alineados con `semanas`; `null` si esa semana no hubo reunión.")
		List<Long> asistentes) {
}
