package com.iglesia.api.asistencia.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.iglesia.api.asistencia.constant.LimitesAsistencia;

@Schema(description = "Datos de un evento (convención, vigilia, bautismo…). Un evento de varios días crea una reunión "
		+ "por día, hasta " + LimitesAsistencia.DIAS_EVENTO_MAX + " días.")
public record CrearEventoRequest(
		@Schema(description = "Nombre del evento; se guarda sin espacios sobrantes.", example = "Convención anual")
		@NotBlank @Size(min = LimitesAsistencia.NOMBRE_EVENTO_MIN, max = LimitesAsistencia.NOMBRE_EVENTO_MAX) String nombre,
		@Schema(description = "Primer día del evento (puede ser futuro).", example = "2026-11-20") @NotNull LocalDate fechaInicio,
		@Schema(description = "Último día del evento. Si se omite, dura un solo día. De 0 a 6 días después de "
				+ "`fechaInicio`.", example = "2026-11-22") LocalDate fechaFin,
		@Schema(description = "Hora de inicio de cada día (HH:mm).", example = "19:00", type = "string",
				format = "time") LocalTime hora) {
}
