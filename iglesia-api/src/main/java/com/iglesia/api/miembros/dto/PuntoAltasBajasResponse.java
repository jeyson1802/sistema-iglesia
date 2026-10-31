package com.iglesia.api.miembros.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Altas y bajas de miembros de una semana o un mes.")
public record PuntoAltasBajasResponse(
		@Schema(description = "Inicio de la semana (lunes) o del mes (día 1).", example = "2026-10-05") LocalDate inicio,
		@Schema(description = "Miembros que llegaron (por su fecha de ingreso o de registro).", example = "4") long altas,
		@Schema(description = "Miembros desactivados.", example = "1") long bajas) {
}
