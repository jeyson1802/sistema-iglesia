package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.api.dto.ReferenciaResponse;

@Schema(description = "Avance de un consolidador en la ronda.")
public record AvanceConsolidadorResponse(
		@Schema(description = "Consolidador; vacío para los casos sin asignar.") ReferenciaResponse consolidador,
		@Schema(description = "Casos asignados.", example = "7") long asignados,
		@Schema(description = "Casos en que se habló con la persona.", example = "5") long contactados,
		@Schema(description = "Casos cerrados.", example = "3") long cerrados) {
}
