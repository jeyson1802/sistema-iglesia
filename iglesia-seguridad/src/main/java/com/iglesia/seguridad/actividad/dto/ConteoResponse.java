package com.iglesia.seguridad.actividad.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cantidad de usuarios de una categoría.")
public record ConteoResponse(
		@Schema(description = "Categoría (rol base).", example = "LIDER") String clave,
		@Schema(description = "Usuarios distintos.", example = "12") long valor) {
}
