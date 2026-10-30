package com.iglesia.api.consolidacion.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.miembros.model.CondicionMiembro;

@Schema(description = "Persona de un caso, con lo necesario para listarla y contactarla.")
public record PersonaCasoResponse(
		@Schema(description = "Miembro.", example = "40") Long miembroId,
		@Schema(description = "Nombres y apellidos.", example = "Ana Quispe") String nombreCompleto,
		@Schema(description = "Teléfono.", example = "987654321") String telefono,
		@Schema(description = "Condición.", example = "NUEVO") CondicionMiembro condicion,
		@Schema(description = "Fecha de ingreso (o de registro).", example = "2026-10-05") LocalDate fechaIngreso,
		@Schema(description = "Si sigue activa.", example = "true") boolean activo) {
}
