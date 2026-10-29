package com.iglesia.api.asistencia.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.asistencia.model.MedioAsistencia;

@Schema(description = "Miembro que asistió a una reunión.")
public record AsistenteResponse(
		@Schema(description = "Identificador del miembro.", example = "12") Long miembroId,
		@Schema(description = "Nombres y apellidos.", example = "Ana Torres") String nombreCompleto,
		@Schema(description = "Nombre de su nivel, si tiene.", example = "Líder") String nivel,
		@Schema(description = "Grupo al que pertenece, si tiene.", example = "8") Long grupoId,
		@Schema(description = "Cómo se marcó.", example = "QR") MedioAsistencia medio,
		@Schema(description = "Cuándo se marcó.", example = "2026-11-01T15:02:11Z") Instant registradaEn) {
}
