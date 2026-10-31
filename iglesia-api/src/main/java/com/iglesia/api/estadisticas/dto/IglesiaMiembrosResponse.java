package com.iglesia.api.estadisticas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Una iglesia y su cantidad de miembros activos (sin datos de personas).")
public record IglesiaMiembrosResponse(
		@Schema(description = "Identificador de la iglesia.", example = "28") Long iglesiaId,
		@Schema(description = "Nombre comercial de la iglesia.", example = "Iglesia Vida Nueva") String nombre,
		@Schema(description = "Miembros activos.", example = "412") long miembros) {
}
