package com.iglesia.api.asistencia.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.asistencia.model.EstadoOcasion;
import com.iglesia.api.asistencia.model.TipoReunion;

@Schema(description = "Una ocasión del historial de un miembro: un día de culto, una reunión de su grupo o un evento al que asistió.")
public record OcasionResponse(
		@Schema(description = "Fecha.", example = "2026-10-25") LocalDate fecha,
		@Schema(description = "Hora de la reunión; `null` en un día de culto (agrupa varias reuniones).", example = "19:30:00")
		LocalTime hora,
		@Schema(description = "`CULTO` (día de culto), `GRUPO` o `EVENTO`.", example = "CULTO") TipoReunion tipo,
		@Schema(description = "Reunión, o reuniones de culto de ese día separadas por « · ».",
				example = "Culto dominical 9:00 · Culto dominical 11:30") String descripcion,
		@Schema(description = "A cuál de ellas asistió; `null` si faltó.", example = "Culto dominical 11:30") String asistioA,
		@Schema(description = "`ASISTIO`, `FALTO` o `EXTRA` (evento, no cuenta como falta).", example = "ASISTIO")
		EstadoOcasion estado) {
}
