package com.iglesia.api.miembros.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Miembro activo sugerido por el selector de miembro.")
public record OpcionMiembroResponse(
		@Schema(description = "Identificador del miembro.", example = "12") Long id,
		@Schema(description = "Nombres y apellidos.", example = "Ana Torres") String nombreCompleto) {
}
