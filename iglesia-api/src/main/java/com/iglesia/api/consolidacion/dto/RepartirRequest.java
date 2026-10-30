package com.iglesia.api.consolidacion.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Reparto de una ronda: balanceado entre los consolidadores elegidos o con las asignaciones indicadas (la propuesta ajustada).")
public record RepartirRequest(
		@Schema(description = "Consolidadores entre los que se reparte; vacío, todos los activos del equipo.", example = "[25, 31]") List<@NotNull Long> consolidadorIds,
		@Schema(description = "Si se reparten solo los casos que aún no tienen consolidador.", example = "true") boolean soloSinAsignar,
		@Schema(description = "Asignaciones exactas; si se envían, se ignoran los demás campos.") @Valid List<AsignacionRequest> asignaciones) {
}
