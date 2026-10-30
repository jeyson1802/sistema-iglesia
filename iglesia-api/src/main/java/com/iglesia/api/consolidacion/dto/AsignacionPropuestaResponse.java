package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Caso con el consolidador que le tocaría en el reparto.")
public record AsignacionPropuestaResponse(
		@Schema(description = "Caso.", example = "310") Long casoId,
		@Schema(description = "Persona del caso.") PersonaBreveResponse persona,
		@Schema(description = "Consolidador propuesto.", example = "25") Long consolidadorId) {
}
