package com.iglesia.api.aportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Total de los aportes vigentes de una semana o un mes, en la moneda de la iglesia.")
public record PuntoAportesResponse(
		@Schema(description = "Inicio de la semana (lunes) o del mes (día 1).", example = "2026-10-05") LocalDate inicio,
		@Schema(description = "Suma de los aportes.", example = "1250.00") BigDecimal total,
		@Schema(description = "Cantidad de aportes.", example = "18") long cantidad) {
}
