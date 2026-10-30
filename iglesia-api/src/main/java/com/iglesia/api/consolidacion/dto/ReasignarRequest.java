package com.iglesia.api.consolidacion.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Consolidadores para uno o varios casos abiertos.")
public record ReasignarRequest(
		@Schema(description = "Casos abiertos que se reasignan.", example = "[310, 311]") @NotEmpty List<@NotNull Long> casoIds,
		@Schema(description = "Miembro activo que pasa a consolidarlos.", example = "25") @NotNull Long consolidadorId) {
}
