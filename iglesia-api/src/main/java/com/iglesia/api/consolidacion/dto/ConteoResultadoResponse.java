package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.api.dto.ReferenciaResponse;

@Schema(description = "Casos cerrados con un resultado.")
public record ConteoResultadoResponse(
		@Schema(description = "Resultado; vacío para los no concluidos.") ReferenciaResponse resultado,
		@Schema(description = "Casos.", example = "6") long cantidad) {
}
