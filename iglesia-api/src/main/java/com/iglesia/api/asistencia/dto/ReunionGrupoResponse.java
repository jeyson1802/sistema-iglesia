package com.iglesia.api.asistencia.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumen de una reunión de grupo: cuántos integrantes asistieron, cuántos faltaron y cuántos "
		+ "visitantes hubo.")
public record ReunionGrupoResponse(
		@Schema(description = "Identificador de la reunión.", example = "120") Long reunionId,
		@Schema(description = "Fecha de la reunión.", example = "2026-10-29") LocalDate fecha,
		@Schema(description = "Hora de inicio, si se conoce.", example = "19:30:00", type = "string",
				format = "time") LocalTime hora,
		@Schema(description = "Integrantes a los que les correspondía la reunión: los activos del grupo que ya habían "
				+ "ingresado a la iglesia ese día, más los que asistieron.", example = "12") long esperados,
		@Schema(description = "Integrantes que asistieron.", example = "9") long asistieron,
		@Schema(description = "Integrantes que no asistieron.", example = "3") long faltaron,
		@Schema(description = "Asistentes que no son integrantes del grupo.", example = "2") long visitantes,
		@Schema(description = "Porcentaje de los integrantes esperados que asistió (0 a 100); nulo si no se esperaba "
				+ "a nadie.", example = "75") Integer porcentaje) {
}
