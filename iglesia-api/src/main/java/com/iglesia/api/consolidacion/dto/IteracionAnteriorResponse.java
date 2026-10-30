package com.iglesia.api.consolidacion.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.api.dto.ReferenciaResponse;

@Schema(description = "Resultado de la iteración anterior de la persona.")
public record IteracionAnteriorResponse(
		@Schema(description = "Resultado; vacío si no concluyó.") ReferenciaResponse resultado,
		@Schema(description = "Cuándo se cerró.", example = "2026-08-20T15:00:00Z") Instant cerradoEn) {
}
