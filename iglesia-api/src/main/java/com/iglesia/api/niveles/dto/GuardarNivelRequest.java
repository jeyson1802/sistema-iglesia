package com.iglesia.api.niveles.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.iglesia.api.niveles.constant.LimitesNivel;

@Schema(description = "Datos para crear un nivel o renombrarlo.")
public record GuardarNivelRequest(
		@Schema(description = "Nombre del nivel. Único en la iglesia sin distinguir mayúsculas; se guarda sin espacios "
				+ "sobrantes.", example = "Diácono") @NotBlank @Size(max = LimitesNivel.NOMBRE_MAX) String nombre) {
}
