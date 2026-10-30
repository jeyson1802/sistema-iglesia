package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import com.iglesia.api.consolidacion.model.SeccionMisCasos;

@Schema(description = "Filtros de «Mis asignados».")
public record FiltroMisCasos(
		@Schema(description = "Sección: para hoy, sin contactar o en contacto.", example = "PARA_HOY") @NotNull SeccionMisCasos seccion,
		@Schema(description = "Texto contenido en el nombre de la persona.", example = "ana") String q) {
}
