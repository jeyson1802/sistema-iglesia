package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.consolidacion.model.EstadoCaso;

@Schema(description = "Seguimiento abierto de una persona.")
public record CasoAbiertoResponse(
		@Schema(description = "Caso.", example = "310") Long casoId,
		@Schema(description = "Ronda; vacía si es individual.") ReferenciaResponse ronda,
		@Schema(description = "Consolidador.") ReferenciaResponse consolidador,
		@Schema(description = "Estado.", example = "EN_CONTACTO") EstadoCaso estado) {
}
