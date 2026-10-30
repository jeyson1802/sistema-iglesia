package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Miembro activo que se agrega al equipo de consolidación.")
public record AgregarConsolidadorRequest(
		@Schema(description = "Miembro activo de la iglesia.", example = "25") @NotNull Long miembroId) {
}
