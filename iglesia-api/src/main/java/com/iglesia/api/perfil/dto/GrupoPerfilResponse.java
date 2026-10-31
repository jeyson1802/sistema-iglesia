package com.iglesia.api.perfil.dto;

import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.grupos.model.DiaSemana;

@Schema(description = "Grupo al que pertenece el miembro, con su día y hora de reunión.")
public record GrupoPerfilResponse(
		@Schema(description = "Identificador del grupo.", example = "22") Long id,
		@Schema(description = "Nombre del grupo (tipo de grupo de la iglesia y su líder).",
				example = "Célula de Ana Mendoza Silva") String nombre,
		@Schema(description = "Día de reunión, si lo tiene.", example = "JUEVES", nullable = true) DiaSemana diaReunion,
		@Schema(description = "Hora de reunión, si la tiene.", example = "19:30:00", type = "string", format = "time",
				nullable = true) LocalTime horaReunion) {
}
