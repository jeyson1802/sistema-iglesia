package com.iglesia.api.asistencia.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.asistencia.model.TipoReunion;

@Schema(description = "Reunión a la que asistió un miembro.")
public record ReunionAsistidaResponse(
		@Schema(description = "Identificador de la reunión.", example = "120") Long id,
		@Schema(description = "Tipo de reunión.", example = "CULTO") TipoReunion tipo,
		@Schema(description = "Nombre de la reunión.", example = "Culto dominical") String nombre,
		@Schema(description = "Fecha de la reunión.", example = "2026-11-01") LocalDate fecha) {
}
