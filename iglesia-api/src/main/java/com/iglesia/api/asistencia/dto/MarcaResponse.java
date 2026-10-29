package com.iglesia.api.asistencia.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de marcar a un miembro: lo que se muestra en la pantalla de escaneo y el total para "
		+ "actualizar el contador sin otra petición.")
public record MarcaResponse(
		@Schema(description = "Identificador del miembro marcado.", example = "12") Long miembroId,
		@Schema(description = "Nombres y apellidos.", example = "Ana Torres") String nombreCompleto,
		@Schema(description = "Nombre de su nivel, si tiene.", example = "Líder") String nivel,
		@Schema(description = "Grupo al que pertenece, si tiene.", example = "8") Long grupoId,
		@Schema(description = "`true` si ya estaba marcado en esta reunión (no es un error).",
				example = "false") boolean yaRegistrado,
		@Schema(description = "Cuándo quedó marcado (la primera vez, si ya lo estaba).",
				example = "2026-11-01T15:02:11Z") Instant registradaEn,
		@Schema(description = "Total de asistentes de la reunión después de marcar.", example = "129") long totalAsistentes) {
}
