package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.consolidacion.model.EstadoCaso;

@Schema(description = "Filtros de los casos de una ronda.")
public record FiltroCasosRonda(
		@Schema(description = "Estado del caso.", example = "PENDIENTE") EstadoCaso estado,
		@Schema(description = "Casos de este consolidador.", example = "25") Long consolidadorId,
		@Schema(description = "Solo casos sin consolidador.", example = "false") Boolean sinAsignar,
		@Schema(description = "Texto contenido en el nombre de la persona.", example = "ana") String q) {

	public boolean soloSinAsignar() {
		return Boolean.TRUE.equals(sinAsignar);
	}

}
