package com.iglesia.api.niveles.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Nivel (rango) configurable de los miembros de la iglesia.")
public record NivelResponse(
		@Schema(description = "Identificador del nivel.", example = "3") Long id,
		@Schema(description = "Nombre del nivel.", example = "Diácono") String nombre,
		@Schema(description = "Posición en la jerarquía: 1 es el más alto.", example = "2") int orden,
		@Schema(description = "`false` si está desactivado: no se puede asignar, pero se conserva en quien ya lo "
				+ "tiene.", example = "true") boolean activo) {
}
