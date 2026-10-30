package com.iglesia.api.consolidacion.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Nuevo orden de los resultados de consolidación.")
public record OrdenResultadosRequest(
		@Schema(description = "Todos los resultados de la iglesia, en el orden deseado.", example = "[3, 1, 2]") @NotEmpty List<@NotNull Long> ids) {
}
