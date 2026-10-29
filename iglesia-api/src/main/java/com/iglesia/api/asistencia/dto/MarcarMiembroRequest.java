package com.iglesia.api.asistencia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Miembro que se marca a mano.")
public record MarcarMiembroRequest(
		@Schema(description = "Identificador del miembro.", example = "12") @NotNull Long miembroId) {
}
