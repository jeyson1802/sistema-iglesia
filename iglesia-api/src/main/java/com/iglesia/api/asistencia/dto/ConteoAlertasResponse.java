package com.iglesia.api.asistencia.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cuántas personas hay en el alcance y cuántas tienen cada alerta (una persona puede "
		+ "contar en varias).")
public record ConteoAlertasResponse(
		@Schema(description = "Personas en seguimiento del alcance y filtros (con o sin alertas): la opción «Todos».",
				example = "120") long personas,
		@Schema(description = "Personas con al menos una alerta.", example = "23") long conAlertas,
		@Schema(description = "Con «Faltas seguidas».", example = "14") long faltasSeguidas,
		@Schema(description = "Con «Nuevo sin volver».", example = "5") long nuevoSinVolver,
		@Schema(description = "Con «Asistencia en caída».", example = "9") long asistenciaEnCaida) {
}
