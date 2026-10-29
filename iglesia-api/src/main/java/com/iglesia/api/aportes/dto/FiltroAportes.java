package com.iglesia.api.aportes.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import com.iglesia.api.aportes.model.MedioAporte;

/** Parámetros de consulta de {@code GET /api/aportes} y {@code GET /api/aportes/resumen}. */
public record FiltroAportes(
		@Parameter(description = "Primer día del rango (incluido).", example = "2026-10-01", required = true)
		@NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
		@Parameter(description = "Último día del rango (incluido). Hasta 366 días después de `desde`.",
				example = "2026-10-31", required = true) @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
		@Parameter(description = "Solo los aportes de esta categoría.", example = "1") Long categoriaId,
		@Parameter(description = "Solo los aportes con este medio.", example = "YAPE") MedioAporte medio,
		@Parameter(description = "Solo los aportes de este miembro. Un líder ve siempre solo los suyos, y este "
				+ "filtro se ignora.", example = "12") Long miembroId,
		@Parameter(description = "Incluir los aportes anulados en el listado (el resumen nunca los cuenta).",
				example = "false") Boolean incluirAnulados) {
}
