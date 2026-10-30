package com.iglesia.api.asistencia.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Quiénes asistieron y quiénes faltaron a una reunión de grupo, cada lista por nombre.")
public record DetalleReunionGrupoResponse(
		@Schema(description = "Identificador de la reunión.", example = "120") Long reunionId,
		@Schema(description = "Fecha de la reunión.", example = "2026-10-29") LocalDate fecha,
		@Schema(description = "Hora de inicio, si se conoce.", example = "19:30:00", type = "string",
				format = "time") LocalTime hora,
		@Schema(description = "Integrantes que asistieron.") List<PersonaReunionGrupoResponse> asistieron,
		@Schema(description = "Integrantes a los que les correspondía la reunión y no asistieron.")
		List<PersonaReunionGrupoResponse> faltaron,
		@Schema(description = "Asistentes que no son integrantes del grupo.")
		List<PersonaReunionGrupoResponse> visitantes) {
}
