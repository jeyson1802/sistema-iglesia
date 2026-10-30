package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Consolidador asignado a un caso.")
public record AsignacionRequest(
		@Schema(description = "Caso de la ronda.", example = "310") @NotNull Long casoId,
		@Schema(description = "Miembro del equipo de consolidación.", example = "25") @NotNull Long consolidadorId) {
}
