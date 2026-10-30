package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Miembro del equipo de consolidación con su carga.")
public record ConsolidadorResponse(
		@Schema(description = "Miembro.", example = "25") Long miembroId,
		@Schema(description = "Nombres y apellidos.", example = "Luis Rojas") String nombreCompleto,
		@Schema(description = "Si sigue en el equipo (los inactivos no entran en el reparto).", example = "true") boolean activo,
		@Schema(description = "Casos abiertos asignados.", example = "6") long casosAbiertos,
		@Schema(description = "Contactos hechos en los últimos 30 días.", example = "14") long contactosRecientes) {
}
