package com.iglesia.api.asistencia.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.asistencia.model.TipoReunion;

@Schema(description = "Reunión donde se toma asistencia, con su total de asistentes.")
public record ReunionResponse(
		@Schema(description = "Identificador de la reunión.", example = "120") Long id,
		@Schema(description = "De dónde sale la reunión.", example = "CULTO") TipoReunion tipo,
		@Schema(description = "Nombre que tenía el culto, el grupo o el evento ese día.",
				example = "Culto dominical") String nombre,
		@Schema(description = "Fecha de la reunión.", example = "2026-11-01") LocalDate fecha,
		@Schema(description = "Hora de inicio, si se conoce.", example = "10:00:00", type = "string",
				format = "time") LocalTime hora,
		@Schema(description = "Horario de culto, si es un culto.", example = "3") Long cultoProgramadoId,
		@Schema(description = "Grupo, si es la reunión de un grupo.", example = "8") Long grupoId,
		@Schema(description = "Cantidad de asistentes marcados.", example = "128") long totalAsistentes) {
}
