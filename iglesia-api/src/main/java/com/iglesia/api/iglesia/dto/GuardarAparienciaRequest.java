package com.iglesia.api.iglesia.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Apariencia de la iglesia: la ven todos sus usuarios. Los valores admitidos son los del "
		+ "configurador de temas de PrimeNG.")
public record GuardarAparienciaRequest(
		@Schema(description = "Preset de PrimeNG.", example = "Aura",
				allowableValues = { "Aura", "Lara", "Nora" }) @NotBlank String preset,
		@Schema(description = "Paleta del color primario; `noir` usa la superficie como color primario.",
				example = "emerald",
				allowableValues = { "noir", "emerald", "green", "lime", "orange", "amber", "yellow", "teal", "cyan",
						"sky", "blue", "indigo", "violet", "purple", "fuchsia", "pink",
						"rose" }) @NotBlank String colorPrimario,
		@Schema(description = "Paleta de superficie (fondos y bordes). Sin valor: la del preset.", example = "slate",
				allowableValues = { "slate", "gray", "zinc", "neutral", "stone", "soho", "viva",
						"ocean" }) String colorSuperficie,
		@Schema(description = "Tipografía del nombre comercial en la topbar y el footer.", example = "Playfair Display",
				allowableValues = { "Playfair Display", "Cinzel", "Montserrat", "Merriweather",
						"Poppins" }) @NotBlank String tipografia) {
}
