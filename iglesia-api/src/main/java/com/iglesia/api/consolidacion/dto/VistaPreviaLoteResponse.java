package com.iglesia.api.consolidacion.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cuántas personas entrarían en el lote con los filtros.")
public record VistaPreviaLoteResponse(
		@Schema(description = "Personas que se agregarían.", example = "37") long entrarian,
		@Schema(description = "Personas que cumplen los filtros pero ya tienen un seguimiento abierto (no se agregan).", example = "4") long conCasoAbierto,
		@Schema(description = "Personas que se agregarían, por apellidos (como máximo el tope de un lote).") List<PersonaBreveResponse> personas) {
}
