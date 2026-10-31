package com.iglesia.api.estadisticas.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Crecimiento y actividad registrada en todas las iglesias en una semana o un mes.")
public record PuntoSistemaResponse(
		@Schema(description = "Inicio de la semana (lunes) o del mes (día 1).", example = "2026-10-01") LocalDate inicio,
		@Schema(description = "Iglesias dadas de alta.", example = "2") long iglesiasNuevas,
		@Schema(description = "Miembros que llegaron (por fecha de ingreso o de registro).", example = "85") long altas,
		@Schema(description = "Miembros desactivados.", example = "6") long bajas,
		@Schema(description = "Miembros registrados en el sistema.", example = "90") long miembrosRegistrados,
		@Schema(description = "Asistencias marcadas.", example = "2140") long asistenciasMarcadas,
		@Schema(description = "Contactos de consolidación registrados.", example = "310") long contactosConsolidacion) {
}
