package com.iglesia.api.aportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Moneda admitida para los aportes.")
public record MonedaResponse(
		@Schema(description = "Código ISO 4217.", example = "PEN") String codigo,
		@Schema(description = "Nombre en español.", example = "sol peruano") String nombre,
		@Schema(description = "Símbolo para mostrar.", example = "S/") String simbolo) {
}
