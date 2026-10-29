package com.iglesia.api.asistencia.dto;

import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.iglesia.api.asistencia.constant.LimitesAsistencia;
import com.iglesia.api.grupos.model.DiaSemana;

@Schema(description = "Datos para crear o editar un horario de culto.")
public record GuardarCultoRequest(
		@Schema(description = "Nombre del culto. Único en la iglesia sin distinguir mayúsculas; se guarda sin espacios "
				+ "sobrantes.", example = "Culto dominical") @NotBlank @Size(min = LimitesAsistencia.NOMBRE_CULTO_MIN,
						max = LimitesAsistencia.NOMBRE_CULTO_MAX) String nombre,
		@Schema(description = "Día de la semana en que se celebra.", example = "DOMINGO") @NotNull DiaSemana diaSemana,
		@Schema(description = "Hora de inicio (HH:mm).", example = "10:00", type = "string", format = "time")
		@NotNull LocalTime hora) {
}
