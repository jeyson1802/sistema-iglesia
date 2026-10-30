package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de una iteración de consolidación.")
public record ResultadoConsolidacionResponse(
		@Schema(description = "Identificador.", example = "2") Long id,
		@Schema(description = "Nombre.", example = "Volverá a asistir") String nombre,
		@Schema(description = "Posición en el catálogo (1 es el primero).", example = "2") int orden,
		@Schema(description = "Si se puede elegir al cerrar un caso.", example = "true") boolean activo) {
}
