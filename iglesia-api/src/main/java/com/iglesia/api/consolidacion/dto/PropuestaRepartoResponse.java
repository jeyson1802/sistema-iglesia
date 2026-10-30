package com.iglesia.api.consolidacion.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Propuesta de reparto, sin guardar.")
public record PropuestaRepartoResponse(
		@Schema(description = "Asignación propuesta de cada caso.") List<AsignacionPropuestaResponse> asignaciones,
		@Schema(description = "Resumen por consolidador.") List<CargaConsolidadorResponse> cargas) {
}
