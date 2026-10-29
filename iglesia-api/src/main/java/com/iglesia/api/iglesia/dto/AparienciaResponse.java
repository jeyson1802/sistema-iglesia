package com.iglesia.api.iglesia.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Apariencia de la iglesia que el frontend aplica a todos sus usuarios.")
public record AparienciaResponse(@Schema(description = "Preset de PrimeNG.", example = "Aura") String preset,
		@Schema(description = "Paleta del color primario.", example = "emerald") String colorPrimario,
		@Schema(description = "Paleta de superficie; nula = la del preset.", example = "slate") String colorSuperficie,
		@Schema(description = "Tipografía del nombre comercial.", example = "Playfair Display") String tipografia) {
}
