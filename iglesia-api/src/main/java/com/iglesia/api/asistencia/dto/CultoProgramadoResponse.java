package com.iglesia.api.asistencia.dto;

import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.grupos.model.DiaSemana;

@Schema(description = "Horario de culto fijo de la iglesia.")
public record CultoProgramadoResponse(
		@Schema(description = "Identificador del horario de culto.", example = "3") Long id,
		@Schema(description = "Nombre del culto.", example = "Culto dominical") String nombre,
		@Schema(description = "Día de la semana.", example = "DOMINGO") DiaSemana diaSemana,
		@Schema(description = "Hora de inicio.", example = "10:00:00", type = "string", format = "time") LocalTime hora,
		@Schema(description = "Si está activo: solo los activos aparecen en las reuniones de hoy.",
				example = "true") boolean activo) {
}
