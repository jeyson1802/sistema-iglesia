package com.iglesia.api.asistencia.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Seguimiento de la asistencia de un grupo: su evolución y sus integrantes ordenados por riesgo.")
public record SeguimientoGrupoResponse(
		@Schema(description = "Últimas 12 semanas, de la más antigua a la actual.") List<SemanaGrupoResponse> semanas,
		@Schema(description = "Integrantes activos: primero los de alertas más graves y más faltas seguidas.")
		List<PersonaSeguimientoResponse> integrantes) {
}
