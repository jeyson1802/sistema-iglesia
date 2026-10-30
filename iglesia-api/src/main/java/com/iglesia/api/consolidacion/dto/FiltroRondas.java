package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.consolidacion.model.EstadoRonda;

@Schema(description = "Filtros de la lista de rondas.")
public record FiltroRondas(
		@Schema(description = "Estado de la ronda.", example = "EN_CURSO") EstadoRonda estado,
		@Schema(description = "Texto contenido en el nombre.", example = "noviembre") String q) {
}
