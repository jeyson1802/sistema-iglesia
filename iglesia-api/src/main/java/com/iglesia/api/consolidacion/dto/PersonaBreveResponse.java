package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Persona con su nombre para mostrar.")
public record PersonaBreveResponse(
		@Schema(description = "Miembro.", example = "40") Long miembroId,
		@Schema(description = "Nombres y apellidos.", example = "Ana Quispe") String nombreCompleto) {
}
