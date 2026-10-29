package com.iglesia.seguridad.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Iglesia del catálogo (iglesia-api) en la que el administrador del sistema va a operar.")
public record EntrarAIglesiaRequest(
		@Schema(description = "Identificador de la iglesia, tal como lo devuelve `GET /api/iglesias` de iglesia-api.",
				example = "1") @NotNull @Positive Long iglesiaId) {
}
