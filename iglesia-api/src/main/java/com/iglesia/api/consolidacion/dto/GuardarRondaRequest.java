package com.iglesia.api.consolidacion.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.iglesia.api.consolidacion.constant.LimitesConsolidacion;

@Schema(description = "Datos para crear o editar una ronda de consolidación.")
public record GuardarRondaRequest(
		@Schema(description = "Nombre de la ronda, único en la iglesia sin distinguir mayúsculas.", example = "Consolidación noviembre 2026") @NotBlank @Size(min = LimitesConsolidacion.NOMBRE_RONDA_MIN, max = LimitesConsolidacion.NOMBRE_RONDA_MAX) String nombre,
		@Schema(description = "Descripción u objetivo de la ronda.", example = "Nuevos que llegaron en octubre") @Size(max = LimitesConsolidacion.DESCRIPCION_RONDA_MAX) String descripcion,
		@Schema(description = "Fecha de inicio.", example = "2026-11-02") @NotNull LocalDate fechaInicio,
		@Schema(description = "Fecha de fin, igual o posterior a la de inicio.", example = "2026-11-30") LocalDate fechaFin) {
}
