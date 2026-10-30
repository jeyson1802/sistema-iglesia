package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de agregar un lote a la ronda.")
public record LoteAgregadoResponse(
		@Schema(description = "Personas agregadas.", example = "37") int agregadas,
		@Schema(description = "Personas que no se agregaron por tener un seguimiento abierto.", example = "4") long conCasoAbierto) {
}
