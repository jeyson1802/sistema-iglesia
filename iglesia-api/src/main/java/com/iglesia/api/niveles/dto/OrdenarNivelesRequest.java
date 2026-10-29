package com.iglesia.api.niveles.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Nuevo orden de los niveles de la iglesia.")
public record OrdenarNivelesRequest(
		@Schema(description = "Ids de **todos** los niveles de la iglesia (activos e inactivos), del más alto al más "
				+ "bajo, sin repetir.", example = "[3, 1, 2]") @NotEmpty List<@NotNull Long> ids) {
}
