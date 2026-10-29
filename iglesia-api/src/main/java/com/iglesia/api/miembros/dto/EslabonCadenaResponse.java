package com.iglesia.api.miembros.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Un líder de la cadena de liderazgo de un miembro.")
public record EslabonCadenaResponse(
		@Schema(description = "Identificador del líder.", example = "12") Long id,
		@Schema(description = "Nombres y apellidos.", example = "Ana Torres") String nombreCompleto,
		@Schema(description = "Nombre de su nivel.", example = "Líder", nullable = true) String nivel,
		@Schema(description = "Si es el pastor principal (el último de la cadena).",
				example = "false") boolean esPastorPrincipal,
		@Schema(description = "Si está activo.", example = "true") boolean activo) {
}
