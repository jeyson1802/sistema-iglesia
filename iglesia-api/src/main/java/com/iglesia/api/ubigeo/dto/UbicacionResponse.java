package com.iglesia.api.ubigeo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ubicación completa deducida del código de distrito.")
public record UbicacionResponse(
		@Schema(description = "Departamento del distrito.") UbigeoResponse departamento,
		@Schema(description = "Provincia del distrito.") UbigeoResponse provincia,
		@Schema(description = "Distrito.") UbigeoResponse distrito) {
}
