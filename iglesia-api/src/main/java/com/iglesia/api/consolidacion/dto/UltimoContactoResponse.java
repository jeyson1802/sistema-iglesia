package com.iglesia.api.consolidacion.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.consolidacion.model.MedioContacto;
import com.iglesia.api.consolidacion.model.ResultadoContacto;

@Schema(description = "Último contacto de un caso.")
public record UltimoContactoResponse(
		@Schema(description = "Cuándo fue.", example = "2026-11-03T19:30:00Z") Instant fechaHora,
		@Schema(description = "Medio.", example = "LLAMADA") MedioContacto medio,
		@Schema(description = "Qué pasó.", example = "NO_RESPONDIO") ResultadoContacto resultado) {
}
