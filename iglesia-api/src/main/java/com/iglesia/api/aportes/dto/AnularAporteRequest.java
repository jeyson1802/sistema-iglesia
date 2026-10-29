package com.iglesia.api.aportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.iglesia.api.aportes.constant.LimitesAporte;

@Schema(description = "Motivo por el que se anula un aporte.")
public record AnularAporteRequest(
		@Schema(description = "Por qué se anula; queda en el historial del aporte.", example = "Registrado dos veces")
		@NotBlank @Size(max = LimitesAporte.MOTIVO_ANULACION_MAX) String motivo) {
}
