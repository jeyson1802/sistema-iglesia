package com.iglesia.api.asistencia.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.format.annotation.DateTimeFormat;

import com.iglesia.api.asistencia.model.TipoReunion;

/** Parámetros de consulta de {@code GET /api/reuniones}. Un filtro omitido no restringe. */
public record FiltroReuniones(
		@Parameter(description = "Primer día del rango (incluido).", example = "2026-10-01")
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
		@Parameter(description = "Último día del rango (incluido). No puede ser anterior a `desde`.",
				example = "2026-10-31") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
		@Parameter(description = "Solo las reuniones de este tipo.", example = "CULTO") TipoReunion tipo,
		@Parameter(description = "Solo las reuniones de este grupo.", example = "8") Long grupoId) {
}
