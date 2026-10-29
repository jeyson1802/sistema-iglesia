package com.iglesia.api.asistencia.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ocasiones de una semana (de lunes a domingo): cuántas correspondían y a cuántas se asistió.")
public record SemanaAsistenciaResponse(
		@Schema(description = "Lunes de la semana.", example = "2026-10-26") LocalDate inicio,
		@Schema(description = "Ocasiones que correspondían (días de culto y reuniones de grupo).", example = "2") long esperadas,
		@Schema(description = "A cuántas de ellas se asistió.", example = "1") long asistidas,
		@Schema(description = "Porcentaje de 0 a 100; `null` si esa semana no correspondía ninguna ocasión.", example = "50")
		Integer porcentaje) {
}
