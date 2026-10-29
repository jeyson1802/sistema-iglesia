package com.iglesia.api.asistencia.dto;

import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.asistencia.model.TipoReunion;

@Schema(description = "Reunión de hoy: abierta (con `id`) o por abrir (sin `id`, con su culto o grupo para abrirla "
		+ "con `POST /api/reuniones`).")
public record ReunionHoyResponse(
		@Schema(description = "Identificador de la reunión, o `null` si aún no se abrió.", example = "120") Long id,
		@Schema(description = "De dónde sale la reunión.", example = "GRUPO") TipoReunion tipo,
		@Schema(description = "Nombre del culto, del grupo o del evento.", example = "Hogar de paz de Ana Torres") String nombre,
		@Schema(description = "Hora de inicio, si se conoce.", example = "19:30:00", type = "string",
				format = "time") LocalTime hora,
		@Schema(description = "Horario de culto, si es un culto.", example = "3") Long cultoProgramadoId,
		@Schema(description = "Grupo, si es la reunión de un grupo.", example = "8") Long grupoId,
		@Schema(description = "Cantidad de asistentes marcados (0 si aún no se abrió).", example = "12") long totalAsistentes) {
}
