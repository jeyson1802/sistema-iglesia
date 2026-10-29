package com.iglesia.api.aportes.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.api.dto.ReferenciaResponse;

@Schema(description = "Total de una categoría en el rango consultado.")
public record TotalCategoriaResponse(@Schema(description = "Categoría.") ReferenciaResponse categoria,
		@Schema(description = "Suma de los aportes vigentes.", example = "1250.00") BigDecimal total,
		@Schema(description = "Cantidad de aportes vigentes.", example = "18") long cantidad) {
}
