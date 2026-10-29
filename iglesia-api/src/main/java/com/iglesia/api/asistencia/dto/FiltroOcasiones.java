package com.iglesia.api.asistencia.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.format.annotation.DateTimeFormat;

import com.iglesia.api.asistencia.model.EstadoOcasion;

/** Parámetros de consulta de {@code GET /api/miembros/{id}/asistencia/ocasiones}. Un filtro omitido no restringe. */
public record FiltroOcasiones(
		@Parameter(description = "Primer día (incluido). Nunca antes de su fecha de ingreso.", example = "2026-08-01")
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
		@Parameter(description = "Último día (incluido); por defecto, hoy.", example = "2026-10-31")
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
		@Parameter(description = "Solo las ocasiones con este estado.", example = "FALTO") EstadoOcasion estado) {
}
