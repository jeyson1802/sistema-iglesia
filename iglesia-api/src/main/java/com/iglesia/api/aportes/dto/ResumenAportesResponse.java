package com.iglesia.api.aportes.dto;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Totales de los aportes vigentes (sin anulados) del rango consultado.")
public record ResumenAportesResponse(
		@Schema(description = "Moneda de la iglesia (ISO 4217).", example = "PEN") String moneda,
		@Schema(description = "Suma de todos los aportes del rango.", example = "3420.50") BigDecimal total,
		@Schema(description = "Cantidad de aportes del rango.", example = "41") long cantidad,
		@Schema(description = "Suma del período anterior de la misma duración, para comparar.",
				example = "2980.00") BigDecimal totalPeriodoAnterior,
		@Schema(description = "Desglose por categoría, en el orden de las categorías.") List<TotalCategoriaResponse> porCategoria,
		@Schema(description = "Desglose por medio.") List<TotalMedioResponse> porMedio) {
}
