package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.iglesia.api.consolidacion.constant.LimitesConsolidacion;

@Schema(description = "Cierre de un caso con el resultado de la iteración.")
public record CerrarCasoRequest(
		@Schema(description = "Resultado activo del catálogo de la iglesia.", example = "2") @NotNull Long resultadoId,
		@Schema(description = "Observación final.", example = "Se comprometió a venir el domingo con su esposa") @Size(max = LimitesConsolidacion.OBSERVACIONES_MAX) String observacion) {
}
