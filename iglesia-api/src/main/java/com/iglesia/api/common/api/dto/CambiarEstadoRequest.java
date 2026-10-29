package com.iglesia.api.common.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Nuevo estado de un registro: `false` lo desactiva y `true` lo reactiva.")
public record CambiarEstadoRequest(
		@Schema(description = "`true` para reactivar, `false` para desactivar.", example = "false") @NotNull Boolean activo) {
}
