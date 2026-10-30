package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Consolidador para un caso abierto.")
public record AsignarConsolidadorRequest(
		@Schema(description = "Miembro activo que pasa a consolidar el caso.", example = "25") @NotNull Long consolidadorId) {
}
