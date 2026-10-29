package com.iglesia.api.aportes.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.aportes.model.MedioAporte;

@Schema(description = "Total de un medio en el rango consultado.")
public record TotalMedioResponse(@Schema(description = "Medio.", example = "YAPE") MedioAporte medio,
		@Schema(description = "Suma de los aportes vigentes.", example = "480.00") BigDecimal total,
		@Schema(description = "Cantidad de aportes vigentes.", example = "9") long cantidad) {
}
