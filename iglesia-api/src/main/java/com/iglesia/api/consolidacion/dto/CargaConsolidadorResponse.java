package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cuántos casos tendría cada consolidador.")
public record CargaConsolidadorResponse(
		@Schema(description = "Consolidador.", example = "25") Long consolidadorId,
		@Schema(description = "Nombres y apellidos.", example = "Luis Rojas") String nombreCompleto,
		@Schema(description = "Casos abiertos antes del reparto.", example = "2") long cargaActual,
		@Schema(description = "Casos que recibiría.", example = "7") long nuevos) {
}
