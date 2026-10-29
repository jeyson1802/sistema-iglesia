package com.iglesia.api.aportes.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Nuevo orden de las categorías de aporte de la iglesia.")
public record OrdenarCategoriasAporteRequest(
		@Schema(description = "Ids de **todas** las categorías de la iglesia (activas e inactivas), en el nuevo orden, "
				+ "sin repetir.", example = "[2, 1, 3]") @NotEmpty List<@NotNull Long> ids) {
}
