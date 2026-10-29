package com.iglesia.api.asistencia.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evolución de los servicios de la iglesia en las últimas 12 semanas. Cada lista va alineada con `semanas`.")
public record ServiciosSeguimientoResponse(
		@Schema(description = "Lunes de cada semana, de la más antigua a la actual.") List<LocalDate> semanas,
		@Schema(description = "Asistentes de cada culto programado.") List<SerieCultoResponse> cultos,
		@Schema(description = "Personas distintas que asistieron a algún culto de la semana.") List<Long> personasEnCultos,
		@Schema(description = "Asistencias en las reuniones de grupo de la semana.") List<Long> presentesEnGrupos,
		@Schema(description = "Porcentaje de las reuniones de grupo que correspondían a sus integrantes y a las que "
				+ "asistieron; `null` si no hubo reuniones.") List<Integer> porcentajeGrupos) {
}
