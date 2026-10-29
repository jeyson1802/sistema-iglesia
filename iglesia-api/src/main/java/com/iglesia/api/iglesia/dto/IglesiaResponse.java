package com.iglesia.api.iglesia.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Iglesia del catálogo de tenants.")
public record IglesiaResponse(
		@Schema(description = "Identificador de la iglesia; es el `iglesia_id` de los tokens y de los datos de "
				+ "negocio.", example = "1") Long id,
		@Schema(description = "Nombre de la iglesia.", example = "Iglesia Central") String nombre,
		@Schema(description = "Instante del alta.", example = "2026-10-28T15:30:00Z",
				format = "date-time") Instant creadaEn) {
}
