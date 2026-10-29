package com.iglesia.api.asistencia.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Asistencia de un grupo en una semana (de lunes a domingo).")
public record SemanaGrupoResponse(
		@Schema(description = "Lunes de la semana.", example = "2026-10-26") LocalDate inicio,
		@Schema(description = "Reuniones del grupo abiertas esa semana.", example = "1") long reuniones,
		@Schema(description = "Asistentes en total, integrantes o no.", example = "11") long asistentes,
		@Schema(description = "Porcentaje de sus integrantes que asistió; `null` si no hubo reunión.", example = "73")
		Integer porcentaje) {
}
