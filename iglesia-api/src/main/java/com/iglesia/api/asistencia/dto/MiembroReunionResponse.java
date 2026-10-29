package com.iglesia.api.asistencia.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Miembro en la toma de asistencia manual, con si ya está marcado en la reunión.")
public record MiembroReunionResponse(
		@Schema(description = "Identificador del miembro.", example = "12") Long miembroId,
		@Schema(description = "Nombres y apellidos.", example = "Ana Torres") String nombreCompleto,
		@Schema(description = "Nombre de su nivel, si tiene.", example = "Líder") String nivel,
		@Schema(description = "Grupo al que pertenece, si tiene.", example = "8") Long grupoId,
		@Schema(description = "Si es integrante del grupo de la reunión (en las reuniones de grupo); `false` para "
				+ "quien vino de otro grupo.", example = "true") boolean integrante,
		@Schema(description = "Si ya está marcado como presente.", example = "true") boolean presente) {
}
