package com.iglesia.api.aportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Configuración de los aportes de la iglesia.")
public record ConfiguracionAportesResponse(
		@Schema(description = "Código ISO 4217 de la moneda de los aportes. `PEN` si la iglesia no la configuró.",
				example = "PEN") String moneda,
		@Schema(description = "`false` si ya hay aportes vigentes: la moneda ya no se puede cambiar.",
				example = "true") boolean monedaEditable) {
}
