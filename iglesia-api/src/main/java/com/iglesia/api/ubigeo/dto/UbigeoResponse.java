package com.iglesia.api.ubigeo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Departamento, provincia o distrito del ubigeo oficial del INEI.")
public record UbigeoResponse(
		@Schema(description = "Código INEI: 2 dígitos el departamento, 4 la provincia y 6 el distrito.",
				example = "150101") String codigo,
		@Schema(description = "Nombre oficial, en mayúsculas.", example = "LIMA") String nombre) {
}
