package com.iglesia.api.aportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Moneda que la iglesia usa para sus aportes.")
public record GuardarConfiguracionAportesRequest(
		@Schema(description = "Código ISO 4217, uno de los que devuelve `GET /api/aportes/monedas`.", example = "USD")
		@NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "debe ser un código de moneda de 3 letras mayúsculas") String moneda) {
}
