package com.iglesia.api.asistencia.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Persona en el detalle de una reunión de grupo.")
public record PersonaReunionGrupoResponse(
		@Schema(description = "Identificador del miembro.", example = "12") Long miembroId,
		@Schema(description = "Nombres y apellidos.", example = "Ana Torres") String nombreCompleto,
		@Schema(description = "Nombre de su nivel, si tiene.", example = "Líder") String nivel) {
}
