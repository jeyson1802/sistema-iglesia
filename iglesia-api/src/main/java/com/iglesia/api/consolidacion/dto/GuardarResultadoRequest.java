package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.iglesia.api.consolidacion.constant.LimitesConsolidacion;

@Schema(description = "Nombre de un resultado de consolidación.")
public record GuardarResultadoRequest(
		@Schema(description = "Nombre del resultado, único en la iglesia sin distinguir mayúsculas.", example = "Volverá a asistir") @NotBlank @Size(min = LimitesConsolidacion.NOMBRE_RESULTADO_MIN, max = LimitesConsolidacion.NOMBRE_RESULTADO_MAX) String nombre) {
}
